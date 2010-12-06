package org.technbolts.gridi.amqp

import collection.JavaConversions
import java.util.concurrent.locks.ReentrantLock
import java.lang.String
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Envelope, DefaultConsumer, Channel, ConnectionFactory}
import collection.mutable.{HashMap, ListBuffer}

object AMQP {
  sealed abstract class ConnectParam(val sym: Symbol)
  object ConnectParam {
    val DefaultHost = Host("127.0.0.1")
    val DefaultPort = Port(5672)
    val DefaultUser = User("guest")
    val DefaultPass = Pass("guest")
    val DefaultVirtualHost = VirtualHost("/")
    case class Host(host: String) extends ConnectParam('host)
    case class Port(port: Int) extends ConnectParam('port)
    case class VirtualHost(vhost: String) extends ConnectParam('vhost)
    case class User(user: String) extends ConnectParam('user)
    case class Pass(pass: String) extends ConnectParam('pass)

    private[amqp] def asMap(params: List[ConnectParam]) = {
      import collection.mutable.HashMap
      val map = new HashMap[Symbol, ConnectParam]
      params.foreach(p => map.put(p.sym, p))
      map.toMap
    }

    private[amqp] def getOrElse[T](map: Map[Symbol, ConnectParam], key: Symbol, defValue: T): T =
      map.get(key) match {
        case None => defValue
        case Some(x: T) => x
        case _ => throw new IllegalStateException("Type mismatch for key " + key)
      }
  }

  def connect(params: ConnectParam*): AMQP = {
    import AMQP.ConnectParam._
    val map = asMap(params.toList)

    val factory = new ConnectionFactory
    factory.setHost(getOrElse(map, 'host, DefaultHost).host)
    factory.setPort(getOrElse(map, 'port, DefaultPort).port)
    factory.setUsername(getOrElse(map, 'user, DefaultUser).user)
    factory.setPassword(getOrElse(map, 'pass, DefaultPass).pass)
    factory.setVirtualHost(getOrElse(map, 'vhost, DefaultVirtualHost).vhost)
    new AMQP(factory)
  }
}

class AMQP(val connectionFactory: ConnectionFactory) {
  def direct(exchangeName: String) = new DirectExchangeBldr(this, exchangeName)

  def fanout(exchangeName: String) = new FanoutExchangeBldr(this, exchangeName)

  def topic(exchangeName: String) = new TopicExchangeBldr(this, exchangeName)

  def queue(queueName: String) = new QueueBldr(this, queueName)
}

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *   Exchange
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

sealed abstract class ExchangeBldr[T <: ExchangeBldr[T]](val amqp: AMQP, val exchangeName: String) {
  self: T =>
  type ChannelInitializer = (Channel)=>Unit
  import Param._
  import collection.mutable.{HashMap}

  def exchangeType: ExchangeType

  private val arguments = new HashMap[String, Any]
  private var durable = DefaultDurable.durable
  private var autoDelete = DefaultAutoDelete.autoDelete
  private var fallbackRoutingKey = RoutingKey(exchangeName)
  private var initializer:Option[ChannelInitializer] = None

  def using(params: ExchangeParam*): T = {
    params.foreach(_ match {
      case Durable(d) => durable = d
      case AutoDelete(d) => autoDelete = d
      case Arguments(values) => values.foreach(e => arguments.put(e._1, e._2))
      case FallbackRoutingKey(routingKey) => fallbackRoutingKey = routingKey
    })
    self
  }

  def durable(durable:Boolean):T = {
    this.durable = durable
    self
  }

  def autoDelete(autoDelete:Boolean):T = {
    this.autoDelete = autoDelete
    self
  }

  def fallback(routingKey:RoutingKey):T = {
    this.fallbackRoutingKey = routingKey
    self
  }

  def initializer(callback:ChannelInitializer):T = {
    initializer = Some(callback)
    self
  }

  def start: Exchange = new ExchangeImpl(amqp, exchangeName, fallbackRoutingKey, List(declareInitializer)).initialize

  def declareInitializer: (Channel)=>Unit = {

    val exType = exchangeType.key
    val args = JavaConversions.asMap(arguments.map(e => (e._1, e._2.asInstanceOf[Object])))
    val exName = exchangeName
    val isDurable = durable
    val isAutoDel = autoDelete
    val initlzr = initializer.getOrElse(RabbitMQSupport.noOpInitializer)

    (channel:Channel) => {
      try {
        channel.exchangeDeclare(exName, exType, isDurable, isAutoDel, args)
        initlzr(channel)
      }
      catch{
        case e => e.printStackTrace
      }
    }
  }
}

class DirectExchangeBldr private[amqp](amqp: AMQP, exchangeName: String) extends ExchangeBldr[DirectExchangeBldr](amqp, exchangeName) {
  val exchangeType = ExchangeType.Direct
}
class FanoutExchangeBldr private[amqp](amqp: AMQP, exchangeName: String) extends ExchangeBldr[FanoutExchangeBldr](amqp, exchangeName) {
  val exchangeType = ExchangeType.Fanout
}
class TopicExchangeBldr private[amqp](amqp: AMQP, exchangeName: String) extends ExchangeBldr[TopicExchangeBldr](amqp, exchangeName) {
  val exchangeType = ExchangeType.Topic
}

class ExchangeImpl private[amqp](
        amqp: AMQP,
        val exchangeName: String,
        val fallbackRoutingKey:RoutingKey,
        channelInitializers:List[(Channel)=>Unit]) extends Exchange with CxManager {

  import org.technbolts.gridi.amqp.{Message => Msg}

  override def configure(channel: Channel):Unit = channelInitializers.foreach( f => f(channel))

  def initialize:ExchangeImpl = {
    getChannel
    this
  }

  def publish(message: Msg):Unit = {
    val routingKey = message.routingKey match {
      case None => fallbackRoutingKey.key
      case Some(r) => r.key
    }
    val props = RabbitMQSupport.paramsToBasicProperties(message.params)
    getChannel.basicPublish(exchangeName, routingKey, props, message.content)
  }

  def connectionFactory = amqp.connectionFactory
}

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *   Queue
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

class QueueBldr private[amqp](amqp: AMQP, val queueName: String) {

  type ChannelInitializer = (Channel)=>Unit
  import Param._
  import com.rabbitmq.client.{Connection, Channel}

  private val bindings = ListBuffer[ChannelInitializer]()

  private var initializer:Option[ChannelInitializer] = None

  def initializer(callback:ChannelInitializer): QueueBldr = {
    initializer = Some(callback)
    this
  }

  def bindTo(direct: DirectExchangeBldr, routingKeys: RoutingKey*): QueueBldr = {
    bindings += { channel:Channel =>
      routingKeys.foreach( r => channel.queueBind(queueName, direct.exchangeName, r.key) )
    }
    this
  }

  def bindTo(exchangeName: String, routingKeys: RoutingKey*): QueueBldr = {
    bindings += { channel:Channel =>
      routingKeys.foreach( r => channel.queueBind(queueName, exchangeName, r.key) )
    }
    this
  }

  def bindTo(fanout: FanoutExchangeBldr): QueueBldr = {
    bindings += { channel:Channel =>
      channel.queueBind(queueName, fanout.exchangeName, "")
    }
    this
  }

  def bindTo(exchangeName: String): QueueBldr = {
    bindings += { channel:Channel =>
      channel.queueBind(queueName, exchangeName, "")
    }
    this
  }

  def bindTo(topic: TopicExchangeBldr, routingPatterns: RoutingPattern*): QueueBldr = {
    bindings += { channel:Channel =>
      routingPatterns.foreach( r => channel.queueBind(queueName, topic.exchangeName, r.pattern) )
    }
    this
  }

  def bindTo(exchangeName:String, routingPattern: RoutingPattern, routingPatterns: RoutingPattern*): QueueBldr = {
    bindings += { channel:Channel =>
      channel.queueBind(queueName, exchangeName, routingPattern.pattern)
      routingPatterns.foreach( r => channel.queueBind(queueName, exchangeName, r.pattern) )
    }
    this
  }
  
  import Param._
  private val arguments = new HashMap[String, Any]
  private var durable = DefaultDurable.durable
  private var exclusive = DefaultExclusive.exclusive
  private var autoDelete = DefaultAutoDelete.autoDelete
  private var autoAck = DefaultAutoAck.autoAck

  def using(params: QueueParam*): QueueBldr = {
    params.foreach(_ match {
      case Durable(d) => durable = d
      case AutoDelete(d) => autoDelete = d
      case Arguments(values) => values.foreach(e => arguments.put(e._1, e._2))
      case Exclusive(e) => exclusive = e
      case AutoAck(a) => autoAck = a
    })
    this
  }

  def durable(durable:Boolean):QueueBldr = {
    this.durable = durable
    this
  }

  def exclusive(exclusive:Boolean):QueueBldr = {
    this.exclusive = exclusive
    this
  }

  def autoDelete(autoDelete:Boolean):QueueBldr = {
    this.autoDelete = autoDelete
    this
  }

  def autoAck(autoAck:Boolean):QueueBldr = {
    this.autoAck = autoAck
    this
  }

  var qos:Option[Int] = None
  def qualityOfService(prefetchCount:Int):QueueBldr = {
    this.qos = Some(prefetchCount)
    this
  }

  def declareQueue:Unit = {
    import RabbitMQSupport._

    // declare the queue
    within(amqp.connectionFactory.newConnection) { channel =>
      val args = JavaConversions.asMap(arguments.map(e => (e._1, e._2.asInstanceOf[Object])))
      channel.queueDeclare(queueName, durable, exclusive, autoDelete, args)
      bindings.foreach(f => f(channel))
    }
  }

  protected def createInitializers:List[ChannelInitializer] = {
    val initializers = ListBuffer[ChannelInitializer]()
    //
    if(qos.isDefined) {
      val prefetch = qos.get
      initializers += { channel => channel.basicQos(prefetch) }
    }

    if(initializer.isDefined)
      initializers += initializer.get
    initializers.toList
  }

  def start: Queue = {
    declareQueue
    new QueueImpl(amqp, autoAck, queueName, createInitializers).connect
  }
}

class QueueImpl private[amqp](
        amqp: AMQP,
        val autoAck:Boolean,
        val queueName: String,
        val channelInitializers:List[(Channel)=>Unit]) extends Queue with CxManager {

  def connectionFactory = amqp.connectionFactory

  override def configure(channel: Channel) = {
    channelInitializers.foreach( initlzr => initlzr(channel))
    channel.basicConsume(queueName, autoAck, newConsumer(channel))
  }

  type TConsumer = (Message) => Unit
  import org.technbolts.util.LockSupport.withinLock

  val subscribersLock = new ReentrantLock
  var subscribers:List[TConsumer] = Nil

  def subscribe(callback: TConsumer):Unit = withinLock(subscribersLock) { subscribers ::= callback }

  def connect:QueueImpl = {
    getChannel
    this
  }

  def reconnect:Unit = {
    dispose
    connect
  }

  protected def newConsumer(channel:Channel) = new DefaultConsumer(channel) {
    override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) = {
      import RabbitMQSupport._
      val routingKey:Option[RoutingKey] = if(envelope.getRoutingKey==null)
                                            None
                                          else
                                            Some(RoutingKey(envelope.getRoutingKey))
      val message = new Message(body, routingKey, properties)
      subscribers.foreach(s => s(message))
      if (!autoAck)
        channel.basicAck(envelope.getDeliveryTag, false)
    }
  }
}
