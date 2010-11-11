import com.rabbitmq.client._
import java.util.concurrent.CyclicBarrier
import org.specs.Specification

class RabbitMQSpecs extends Specification {
  "RabbitMQ" should {

    "handle a message produced and then consumed" in {
      val barrier = new CyclicBarrier(2)
      new Thread(new Runnable {
        def run = {
          val producer = new RabbitMQProducer
          System.out.println("Producing...")
          producer.produce
        }
      }).start

      new Thread(new Runnable {
        def run = {
          val consumer = new RabbitMQProducer
          System.out.println("Consuming...")
          consumer.consume(received => {
            System.out.println("Message received: " + received)
            barrier.await
          })
        }
      }).start

      barrier.await
    }
  }
}


class RabbitMQProducer {
  val factory = new ConnectionFactory
  factory.setUsername("guest");
  factory.setPassword("guest");
  factory.setVirtualHost("/");
  factory.setHost("127.0.0.1");
  factory.setPort(5672);
  def produce: Unit = {
    val conn: Connection = factory.newConnection
    val channel: Channel = conn.createChannel
    val exchangeName = "myExchange"
    val routingKey = "testRoute"
    val messageBodyBytes = "Hello, world!".getBytes("utf-8")
    channel.basicPublish(exchangeName, routingKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
    channel.close
    conn.close
  }

  def consume(callback:(String)=>Unit): Unit = {
    val conn: Connection = factory.newConnection
    val channel: Channel = conn.createChannel
    val exchangeName = "myExchange"
    val routingKey = "testRoute"
    val queueName = "myQueue";
    val durable = true;
    channel.exchangeDeclare(exchangeName, "direct", durable);
    channel.queueDeclare(queueName, durable, false, false, null);
    channel.queueBind(queueName, exchangeName, routingKey);
    val noAck = false;
    val consumer = new QueueingConsumer(channel);
    channel.basicConsume(queueName, noAck, consumer);
    val runInfinite = true;
    while (runInfinite) {
      try {
        val delivery: QueueingConsumer.Delivery = consumer.nextDelivery
        callback(new String(delivery.getBody, "utf-8"))
        channel.basicAck(delivery.getEnvelope.getDeliveryTag, false)
      } catch {
        case e: InterruptedException => // no op
      }
    }
    channel.close();
    conn.close();
  }
}