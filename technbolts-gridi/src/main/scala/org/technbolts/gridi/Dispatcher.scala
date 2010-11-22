package org.technbolts.gridi

import actors.Actor
import com.rabbitmq.client._
import com.rabbitmq.client.AMQP.BasicProperties
import java.lang.String

class Dispatcher[T](val connectionFactory: ConnectionFactory, val converter: Converter[T]) extends Actor {
  private var subscribers: List[Actor] = Nil


  def act() {
    loop {
      react {
        case AddSubscriber(sub) => subscribers ::= sub
        case RemoveSubscriber(sub) => subscribers = subscribers.filterNot(_ == sub)
        case msg@Dispatch => subscribers.foreach(_ ! msg)
        case params: ConnectToQueue => connectToQueue(params)
        case params: Publish[T] => publish(params)
      }
    }
  }

  def exchangeName = "exchange"

  def routingKey = "routingKey"

  protected def connectToQueue(params: ConnectToQueue) = {
    val conn: Connection = connectionFactory.newConnection
    val channel: Channel = conn.createChannel

    val queueName = params.queueName
    val durable = params.durable
    channel.exchangeDeclare(exchangeName, "direct", durable)
    channel.queueDeclare(queueName, durable, false, false, null)
    channel.queueBind(queueName, exchangeName, routingKey)
    channel.basicConsume(queueName, params.noAck, newConsumer(channel, params.noAck))
  }

  protected def newConsumer(channel:Channel, noAck:Boolean): Consumer =
    new DispatcherConsumer(channel, this, converter, noAck)

  protected def publish(params: Publish[T]) = {
    val persistent = params.persistent
    val mode = if (converter.text_?) {
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
    val message = converter.convertToBytes(params.message)
    execute(_.basicPublish(exchangeName, routingKey, mode, message))
  }

  import RabbitMQ._

  protected def execute(func: (Channel) => Unit) = {
    val conn: Connection = connectionFactory.newConnection
    try {
      val channel: Channel = conn.createChannel
      try {
        func(channel)
      } finally {
        closeQuietly(channel)
      }
    } finally {
      closeQuietly(conn)
    }
  }
}

class DispatcherConsumer[T](
        val channel: Channel,
        val dispatcher: Actor,
        val converter: Converter[T],
        val noAck: Boolean) extends DefaultConsumer(channel) {
  //
  override def handleDelivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) = {
    converter.convert(Delivery(consumerTag, envelope, properties, body)) match {
      case Some(t) => dispatcher ! Dispatch(t)
      case _ => // no op
    }
    if (!noAck)
      channel.basicAck(envelope.getDeliveryTag, false)
  }
}