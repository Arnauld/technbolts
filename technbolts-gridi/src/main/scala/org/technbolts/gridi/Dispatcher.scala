package org.technbolts.gridi

import actors.Actor
import com.rabbitmq.client._
import com.rabbitmq.client.AMQP.BasicProperties
import java.lang.String
import org.slf4j.{Logger, LoggerFactory}

class Dispatcher[T](val connectionFactory: ConnectionFactory, val converter: Converter[T]) extends Actor {
  val log: Logger = LoggerFactory.getLogger(classOf[Dispatcher[T]])

  private var subscribers: List[Actor] = Nil
  def exchangeName = "exchange"

  def routingKey = "routingKey"

  def act() {
    loop {
      react {
        case AddSubscriber(sub)    => subscribers ::= sub
        case RemoveSubscriber(sub) => subscribers = subscribers.filterNot(_ == sub)
        case msg: Dispatch[T]      => subscribers.foreach(_ ! msg)
        case cnx: ConnectToQueue   => connectToQueue(cnx)
        case msg: Publish[T]       => publish(msg)
        case unknown =>
          log.warn("Unknown dispatcher event received: {}", unknown)
      }
    }
  }


  protected def connectToQueue(params: ConnectToQueue) = {
    val conn: Connection = connectionFactory.newConnection
    val channel: Channel = conn.createChannel

    val queueName = params.queueName
    val durable = params.durable
    channel.exchangeDeclare(exchangeName, "direct", durable)
    channel.queueDeclare(queueName, durable, false, false, null)
    channel.queueBind(queueName, exchangeName, routingKey)
    channel.basicConsume(queueName, params.noAck, newQueueConsumer(channel, queueName, params.noAck))
  }

  protected def newQueueConsumer(channel: Channel, queueName:String, noAck: Boolean): Consumer =
    new DispatcherQueueConsumer(channel, queueName, this, converter, noAck)

  protected def publish(params: Publish[T]) = {
    val mode = computeProps(params.persistent)
    val message = converter.convertToBytes(params.message)
    import RabbitMQ._
    executeInChannel(connectionFactory, _.basicPublish(exchangeName, routingKey, mode, message))
  }
  
  protected def computeProps(persistent:Boolean) = if (converter.text_?) {
      if (persistent)
        MessageProperties.PERSISTENT_TEXT_PLAIN
      else
        MessageProperties.TEXT_PLAIN
    } else {
      if (persistent)
        MessageProperties.PERSISTENT_BASIC
      else
        MessageProperties.BASIC

    }
}

class DispatcherQueueConsumer[T](
        val channel: Channel,
        val queueName: String,
        val dispatcher: Actor,
        val converter: Converter[T],
        val noAck: Boolean) extends DefaultConsumer(channel) {
  val log: Logger = LoggerFactory.getLogger(classOf[DispatcherQueueConsumer[T]])

  //
  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) = {
    converter.convert(Delivery(consumerTag, envelope, properties, body)) match {
      case Some(t) =>
        log.debug("Message received from channel: {}", t)
        dispatcher ! Dispatch(queueName, t)
      case _ => // no op
    }
    if (!noAck)
      channel.basicAck(envelope.getDeliveryTag, false)
  }
}