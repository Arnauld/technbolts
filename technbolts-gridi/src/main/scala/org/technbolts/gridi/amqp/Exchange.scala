package org.technbolts.gridi.amqp

case class ExchangeName(exchangeName:String)

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
sealed trait Message {
  def isPersistent:Boolean = false
  def routingKey:Option[String] = None
}

/**
 * A text based message.<p/>
 */
class TextMessage(body:String) extends Message

/**
 * A text based message.<p/>
 */
class BinaryMessage(body:Array[Byte]) extends Message

/**
 * 
 */
trait Exchange {

  /**
   * Publish a message in the exchange.<p/>
   *
   * <p>
   * Queues and Exchanges can be marked as durable.
   * All this means is that if the server restarts, the queues will still be there.
   * Individual messages have to be sent in delivery mode two for them to survive a server restart.
   * <br/>
   * <code>exchange.publish("message", :persistent=&gt;true)</code>
   * </p>
   */
  def publish(message:Message):Unit
}
