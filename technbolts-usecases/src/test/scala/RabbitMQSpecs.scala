import collection.mutable.ListBuffer
import com.rabbitmq.client._
import java.lang.Throwable
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger}
import java.util.concurrent.{TimeUnit, CyclicBarrier}
import org.slf4j.{LoggerFactory, Logger}
import org.specs.Specification
import org.technbolts.util.Bytes

class RabbitMQSpecs extends Specification {

  val log:Logger = LoggerFactory.getLogger(classOf[RabbitMQSpecs])

  "RabbitMQ" should {

    "handle a message produced and then consumed" in {
      val HELLO = "Hello, it is " + System.currentTimeMillis

      val barrier = new CyclicBarrier(2)
      val terminationCounter = new AtomicInteger
      val group   = new ThreadGroup ("rabbitMQ group") {
        override def uncaughtException(thread: Thread, throwable: Throwable) =
            log.error("Oops what is this? (occured in " + thread + ")", throwable)
      }
      new Thread(group, new Runnable {
        def run = {
          val producer = new RabbitMQProducer
          log.info("Producing...")
          producer.basicPublish(HELLO)
        }
      }).start

      val receivedBuffer = new ListBuffer[String]
      new Thread(group, new Runnable {
        def run = {
          val consumer = new RabbitMQProducer
          log.info("Consuming...")
          consumer.consume(received => {
            log.info("Message received: " + received)
            receivedBuffer += received
            barrier.await
            consumer.stop
          })
        }
      }).start

      barrier.await
      receivedBuffer.size must equalTo(1)
      receivedBuffer(0)   must equalTo(HELLO)
    }
  }
}


object RabbitMQSupport {

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


object RabbitMQ {

  val log:Logger = LoggerFactory.getLogger(classOf[RabbitMQ])

  type DeliveryCallback = (QueueingConsumer.Delivery)=>Unit

  import Bytes._
  implicit def stringToDeliveryCallback(callback:(String)=>Unit):DeliveryCallback =
    (delivery:QueueingConsumer.Delivery) => {
      //log.info("Delivery: " + delivery)
      callback(delivery.getBody)
    }
}

trait RabbitMQ {

  private val log:Logger = LoggerFactory.getLogger(classOf[RabbitMQ])

  def username = "guest"
  def password = "guest"
  def virtualHost = "/"
  def host = "127.0.0.1"
  def port = 5672

  def newConnectionFactory = {
    val factory = new ConnectionFactory
    factory.setUsername(username)
    factory.setPassword(password)
    factory.setVirtualHost(virtualHost)
    factory.setHost(host)
    factory.setPort(port)
    factory
  }

  lazy val connectionFactory = newConnectionFactory

  def exchangeName = "exchange"
  def routingKey = "routingKey"

  import RabbitMQSupport._

  def execute(func:(Channel)=>Unit) = {
    val conn: Connection = connectionFactory.newConnection
    try{
      val channel: Channel = conn.createChannel
      try{
        func(channel)
      }finally{
        closeQuietly(channel)
      }
    }finally{
      closeQuietly(conn)
    }
  }

  import Bytes._

  def basicPublish(message:String):Unit = basicPublish(message, true)
  
  def basicPublish(message:String, persistent:Boolean):Unit = {
    val mode = if(persistent)
                 MessageProperties.PERSISTENT_TEXT_PLAIN
               else
                 MessageProperties.TEXT_PLAIN
    execute(_.basicPublish(exchangeName, routingKey, mode, message))
  }

  def openQueue:RabbitMQQueue = openQueue("defaultQueue", true)

  def openQueue(queueName:String, durable:Boolean):RabbitMQQueue = openQueue(queueName, durable, false)

  def openQueue(queueName:String, durable:Boolean, noAck:Boolean):RabbitMQQueue = {
    val conn: Connection = connectionFactory.newConnection
    val channel: Channel = conn.createChannel

    channel.exchangeDeclare(exchangeName, "direct", durable)
    channel.queueDeclare(queueName, durable, false, false, null)
    channel.queueBind(queueName, exchangeName, routingKey)
    val consumer = new QueueingConsumer(channel)
    channel.basicConsume(queueName, noAck, consumer)

    new RabbitMQQueue(channel, consumer, noAck)
  }

}

class RabbitMQQueue(val channel:Channel, val consumer:QueueingConsumer, val noAck:Boolean) {

  private val stopped = new AtomicBoolean(false)

  type DeliveryConsumer = (QueueingConsumer.Delivery)=>Unit

  def defaultTimeout = 1000L

  def defaultTimeoutUnit = TimeUnit.MILLISECONDS

  def nextDelivery(callback:DeliveryConsumer):Unit = nextDelivery(callback, defaultTimeout, defaultTimeoutUnit)

  def nextDelivery(callback:DeliveryConsumer, timeout:Long, unit:TimeUnit):Unit = {
    val delivery: QueueingConsumer.Delivery = consumer.nextDelivery(unit.toMillis(timeout))
    callback(delivery)
    if(!noAck)
      channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
  }

  def loop(callback:DeliveryConsumer):Unit = loop(callback, defaultTimeout, defaultTimeoutUnit)

  def loop(callback:DeliveryConsumer, timeout:Long, unit:TimeUnit):Unit =
    while (!stopped.get) {
      try{
        nextDelivery(callback, timeout, unit)
      } catch {
        case e: InterruptedException => // no op
      }
    }
}

class RabbitMQProducer extends RabbitMQ {

  val log:Logger = LoggerFactory.getLogger(classOf[RabbitMQProducer])

  private val stopped = new AtomicBoolean(false)

  def stop:Unit = {
    log.info("Stop triggered")
    stopped.set(true)
  }

  def produce: Unit = basicPublish("Hello, world!")

  import RabbitMQ._

  def consume(callback:(String)=>Unit): Unit = {
    val queue = openQueue("myQueue", true)
    queue.loop(callback)
  }
}