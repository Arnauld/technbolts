package org.technbolts.gridi

import org.slf4j.{Logger, LoggerFactory}
import com.rabbitmq.client.{Connection, Channel, ConnectionFactory}

trait RabbitMQ {
  private val log:Logger = LoggerFactory.getLogger(classOf[RabbitMQ])

  def username = "guest"
  def password = "guest"
  def virtualHost = "/"
  def host = "127.0.0.1"
  def port = 5672

  lazy val connectionFactory = newConnectionFactory

  def newConnectionFactory = {
    val factory = new ConnectionFactory
    factory.setUsername(username)
    factory.setPassword(password)
    factory.setVirtualHost(virtualHost)
    factory.setHost(host)
    factory.setPort(port)
    factory
  }
}

object RabbitMQ {

  def newConnectionFactory = new RabbitMQ {}.newConnectionFactory

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