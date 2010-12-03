package org.technbolts.gridi.amqp

import collection.JavaConversions
import java.util.concurrent.locks.ReentrantLock
import java.lang.String
import com.rabbitmq.client.AMQP.BasicProperties
import com.rabbitmq.client.{Envelope, DefaultConsumer, Channel, ConnectionFactory}
import org.technbolts.gridi.amqp.temp.{MessageParam}
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
 *   Message
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
package temp {
import org.technbolts.gridi.amqp.temp.MessageParam._

sealed abstract class MessageParam(val sym: Symbol)
object MessageParam {
  case class ContentType(contentType: String) extends MessageParam('ContentType)
  val TextPlain = ContentType("text/plain")
  val OctetStream = ContentType("application/octet-stream")

  case class ContentEncoding(encoding: String) extends MessageParam('ContentEncoding)
  val Utf8 = ContentEncoding("utf8")

  case class DeliveryMode(mode: Int) extends MessageParam('Persistent)
  val Persistent = DeliveryMode(2)
  val NonPersistent = DeliveryMode(1)

  case class Priority(priority: Int) extends MessageParam('Priority)
  val PriorityZero = Priority(0)

  case class ReplyTo(replyTo: String) extends MessageParam('ReplyTo)

  val Utf8TextPlain = List(TextPlain, Utf8, NonPersistent, PriorityZero)
  val PersistentUtf8TextPlain = List(TextPlain, Utf8, Persistent, PriorityZero)
  val BasicBinary = List(OctetStream, NonPersistent, PriorityZero)
  val PersistentBasicBinary = List(OctetStream, Persistent, PriorityZero)

  import com.rabbitmq.client.AMQP.{BasicProperties => BasicProps }

  implicit def paramsToBasicProperties(params: List[MessageParam]):BasicProps = {
    val properties = new BasicProps
    params.foreach( _ match {
      case ContentType(cType) => properties.setContentType(cType)
      case ContentEncoding(cEncoding) => properties.setContentEncoding(cEncoding)
      case DeliveryMode(mode) => properties.setDeliveryMode(mode)
      case Priority(p) => properties.setPriority(p)
      case ReplyTo(r) => properties.setReplyTo(r)
    })
    properties
  }

  implicit def basicPropertiesToParams(props:BasicProps) : List[MessageParam] = {
    val params = new ListBuffer[MessageParam]
    if(props.getContentType!=null)
      params += ContentType(props.getContentType)
    if(props.getContentEncoding!=null)
      params += ContentEncoding(props.getContentEncoding)
    params.toList
  }
}

/**
 * Base trait for message.<p/>
 *
 * <p>
 * Queues and Exchanges can be marked as durable.
 * All this means is that if the server restarts, the queues will still be there.
 * Individual messages have to be sent in delivery mode two for them to survive a server restart.
 * <br/>
 * <code>exchange.publish("message", :persistent=&gt;true)</code>
 * </p>
 */
case class Message(content: Array[Byte], routingKey: Option[RoutingKey], params: List[MessageParam]) {
  def contentAsString:String =
    params.find( _.sym == 'ContentEncoding) match {
      case None => new String(content)
      case Some(c) => new String(content, c.asInstanceOf[ContentEncoding].encoding)
    }
}

object Message {
  import MessageParam._

  def apply(content: String): Message = new Message(content.getBytes(Utf8.encoding), None, Utf8TextPlain)

  def apply(content: String, contentEncoding: ContentEncoding): Message = {
    val bytes = content.getBytes(contentEncoding.encoding)
    new Message(bytes, None, List(TextPlain, contentEncoding, NonPersistent, PriorityZero))
  }

  def apply(content: Array[Byte]): Message = new Message(content, None, BasicBinary)

  def apply(content: Array[Byte], contentType: ContentType): Message = {
    new Message(content, None, List(contentType, NonPersistent, PriorityZero))
  }

}

trait Exchange {
  def exchangeName:String
  def publish(message: Message): Unit
}
trait Queue {
  def queueName:String
  def subscribe(callback: (Message) => Unit): Unit
}
}

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *   Shared
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

sealed trait Param { def sym:Symbol }
sealed trait ExchangeParam extends Param
sealed trait QueueParam extends Param
sealed abstract class ParamImpl(val sym: Symbol) extends Param
object Param {
  val DefaultDurable = Durable(false)
  val DefaultExclusive = Exclusive(false)
  val DefaultAutoDelete = AutoDelete(false)
  val DefaultArguments = Arguments(Nil)
  val DefaultAutoAck = AutoAck(true)
  case class Durable(durable: Boolean) extends ParamImpl('durable) with ExchangeParam with QueueParam
  case class Exclusive(exclusive: Boolean) extends ParamImpl('durable) with QueueParam
  case class AutoDelete(autoDelete: Boolean) extends ParamImpl('autoDelete) with ExchangeParam with QueueParam
  case class Arguments(values: List[(String, Any)]) extends ParamImpl('arguments) with ExchangeParam with QueueParam
  case class FallbackRoutingKey(key:RoutingKey) extends ParamImpl('fallbackRoutingKey) with ExchangeParam
  case class AutoAck(autoAck: Boolean) extends ParamImpl('autoAck) with QueueParam
}

case class Cx(connection:com.rabbitmq.client.Connection, channel:com.rabbitmq.client.Channel)

trait CxManager {
  import com.rabbitmq.client.{Channel}

  //def channelInitializers:List[(Channel)=>Unit] = Nil
  def configure(channel:Channel):Unit = {}

  def connectionFactory:com.rabbitmq.client.ConnectionFactory

  protected var connection:Option[Cx] = None

  def getChannel:Channel = connection match {
    case Some(cx) => cx.channel
    case None =>
      val connect = connectionFactory.newConnection
      val channel = connect.createChannel
      configure(channel)
      connection = Some(Cx(connect,channel))
      channel
  }

  def dispose:Unit = connection match {
    case Some(cx) =>
      import RabbitMQSupport._
      closeQuietly(cx.channel)
      closeQuietly(cx.connection)
    case _ => // nothing to do
  }
}

object RabbitMQSupport {
  import com.rabbitmq.client.{Connection, Channel}
  def close(a:AnyRef) = a match {
    case c:Channel => c.close
    case c:Connection => c.close
    case x if (x==null) => // no op
    case _ => throw new IllegalArgumentException ("Unsupported type in close")
  }

  def closeQuietly(a:AnyRef) =
    try{
      close(a)
    }catch{
      case e => //ignore we're quiet, chut!
    }
}

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *   Exchange
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

sealed abstract class ExchangeBldr[T <: ExchangeBldr[T]](val amqp: AMQP, val exchangeName: String) {
  self: T =>
  import Param._
  import collection.mutable.{HashMap}

  def exchangeType: ExchangeType

  private val arguments = new HashMap[String, Any]
  private var durable = DefaultDurable.durable
  private var autoDelete = DefaultAutoDelete.autoDelete
  private var fallbackRoutingKey = RoutingKey(exchangeName)

  def using(params: ExchangeParam*): T = {
    params.foreach(_ match {
      case Durable(d) => durable = d
      case AutoDelete(d) => autoDelete = d
      case Arguments(values) => values.foreach(e => arguments.put(e._1, e._2))
      case FallbackRoutingKey(routingKey) => fallbackRoutingKey = routingKey
    })
    self
  }

  def start: temp.Exchange = new ExchangeImpl(amqp, exchangeName, fallbackRoutingKey, List(declareInitializer)).initialize

  def declareInitializer: (Channel)=>Unit = {

    val exType = exchangeType.key
    val args = JavaConversions.asMap(arguments.map(e => (e._1, e._2.asInstanceOf[Object])))
    val exName = exchangeName
    val isDurable = durable
    val isAutoDel = autoDelete

    (channel:Channel) => {
      channel.exchangeDeclare(exName, exType, isDurable, isAutoDel, args)
    }
  }
}

class ExchangeImpl private[amqp](
        amqp: AMQP,
        val exchangeName: String,
        val fallbackRoutingKey:RoutingKey,
        channelInitializers:List[(Channel)=>Unit]) extends temp.Exchange with CxManager {

  import org.technbolts.gridi.amqp.temp.{Message => Msg}

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
    import MessageParam._
    getChannel.basicPublish(exchangeName, routingKey, message.params, message.content)
  }

  def connectionFactory = amqp.connectionFactory
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

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *   Queue
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */

/*

@see com.rabbitmq.client.AMQP.Queue.DeclareOk
     * @param queue the name of the queue
     * @param durable true if we are declaring a durable queue (the queue will survive a server restart)
     * @param exclusive true if we are declaring an exclusive queue (restricted to this connection)
     * @param autoDelete true if we are declaring an autodelete queue (server will delete it when no longer in use)
     * @param arguments other properties (construction arguments) for the queue

 */

class QueueBldr private[amqp](amqp: AMQP, val queueName: String) {

  type ChannelInitializer = (Channel)=>Unit
  import Param._
  import com.rabbitmq.client.{Connection, Channel}

  private var isBound = false
  private val bindings = ListBuffer[ChannelInitializer]()

  def bindTo(direct: DirectExchangeBldr, routingKeys: RoutingKey*): QueueBldr = {
    bindings += { channel:Channel =>
      routingKeys.foreach( r => channel.queueBind(queueName, direct.exchangeName, r.key) )
    }
    this
  }

  def bindTo(fanout: FanoutExchangeBldr): QueueBldr = {
    bindings += { channel:Channel =>
      println("<" + queueName + "//" + fanout.exchangeName + ">")
      channel.queueBind(queueName, fanout.exchangeName, "")
    }
    this
  }

  def bindTo(topic: TopicExchangeBldr, routingPatterns: RoutingPattern*): QueueBldr = {
    bindings += { channel:Channel =>
      routingPatterns.foreach( r => channel.queueBind(queueName, topic.exchangeName, r.pattern) )
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

  def start: temp.Queue = {
    val connection = amqp.connectionFactory.newConnection
    val channel = connection.createChannel
    val args = JavaConversions.asMap(arguments.map(e => (e._1, e._2.asInstanceOf[Object])))
    channel.queueDeclare(queueName, durable, exclusive, autoDelete, args)
    bindings.foreach(f => f(channel))

    val consumer = new QueueImpl(channel, queueName)
    channel.basicConsume(queueName, autoAck, consumer)
    consumer
  }
}

class QueueImpl private[amqp](
        channel: Channel,
        val queueName: String) extends DefaultConsumer(channel) with temp.Queue {
  type TConsumer = (temp.Message) => Unit
  import org.technbolts.util.LockSupport.withinLock

  val subscribersLock = new ReentrantLock
  var subscribers:List[TConsumer] = Nil

  def subscribe(callback: TConsumer):Unit = withinLock(subscribersLock) { subscribers ::= callback }

  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) = {
    import MessageParam._
    var routingKey:Option[RoutingKey] = if(envelope.getRoutingKey==null)
                                          None
                                        else
                                          Some(RoutingKey(envelope.getRoutingKey))
    val message = new temp.Message(body, routingKey, properties)
    subscribers.foreach(s => s(message))
  }

  def dispose:Unit = {
    import RabbitMQSupport._
    val cx = channel.getConnection
    closeQuietly(channel)
    closeQuietly(cx)
  }
}
