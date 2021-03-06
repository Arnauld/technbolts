package org.technbolts.gridi.amqp

import collection.mutable.ListBuffer
import java.util.concurrent.locks.ReentrantLock
import org.technbolts.util.LockSupport

object RabbitMQSupport {
  import com.rabbitmq.client.{Connection, Channel}
  import com.rabbitmq.client.AMQP.{BasicProperties => BasicProps }
  import org.technbolts.gridi.amqp.MessageParam._

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

  val noOpInitializer:(Channel)=>Unit = { channel => /* no op */ }

  def within[T](channel:Channel)(func: (Channel)=>T) {
    try{
      func(channel)
    }
    finally close(channel)
  }

  def within[T](connection:Connection)(func: (Channel)=>T) {
    try{
      within(connection.createChannel)(func)
    }
    finally close(connection)
  }


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

case class Cx(connection:com.rabbitmq.client.Connection, channel:com.rabbitmq.client.Channel)

trait CxManager {
  import com.rabbitmq.client.{Channel}

  //def channelInitializers:List[(Channel)=>Unit] = Nil
  def configure(channel:Channel):Unit = {}

  def connectionFactory:com.rabbitmq.client.ConnectionFactory

  private val cxLock = new ReentrantLock
  protected var connection:Option[Cx] = None

  import LockSupport._
  def getChannel:Channel = withinLock(cxLock) {
    connection match {
      case Some(cx) => cx.channel
      case None =>
        val connect = connectionFactory.newConnection
        val channel = connect.createChannel
        configure(channel)
        connection = Some(Cx(connect,channel))
        channel
    }
  }

  def dispose:Unit = withinLock(cxLock) {
    connection match {
      case Some(cx) =>
        import RabbitMQSupport._
        closeQuietly(cx.channel)
        closeQuietly(cx.connection)
        connection = None
      case _ => // nothing to do
    }
  }
}
