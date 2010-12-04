package org.technbolts.gridi.amqp

/* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 *   Message
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
import org.technbolts.gridi.amqp.MessageParam._

sealed trait MessageParam extends Param
object MessageParam {
  case class ContentType(contentType: String) extends ParamImpl('ContentType) with MessageParam
  val TextPlain = ContentType("text/plain")
  val OctetStream = ContentType("application/octet-stream")

  case class ContentEncoding(encoding: String) extends ParamImpl('ContentEncoding) with MessageParam
  val Utf8 = ContentEncoding("utf8")

  /**
   * <p>
   * Queues and Exchanges can be marked as durable.
   * All this means is that if the server restarts, the queues will still be there.
   * Individual messages have to be sent in delivery mode two for them to survive a server restart.
   * <br/>
   * <code>exchange.publish("message", :persistent=&gt;true)</code>
   * </p>
   */
  case class DeliveryMode(mode: Int) extends ParamImpl('Persistent) with MessageParam
  val Persistent = DeliveryMode(2)
  val NonPersistent = DeliveryMode(1)

  case class Priority(priority: Int) extends ParamImpl('Priority) with MessageParam
  val PriorityZero = Priority(0)

  case class ReplyTo(replyTo: String) extends ParamImpl('ReplyTo) with MessageParam

  val Utf8TextPlain = List(TextPlain, Utf8, NonPersistent, PriorityZero)
  val PersistentUtf8TextPlain = List(TextPlain, Utf8, Persistent, PriorityZero)
  val BasicBinary = List(OctetStream, NonPersistent, PriorityZero)
  val PersistentBasicBinary = List(OctetStream, Persistent, PriorityZero)
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