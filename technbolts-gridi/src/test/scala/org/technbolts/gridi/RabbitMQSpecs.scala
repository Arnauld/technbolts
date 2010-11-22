package org.technbolts.gridi

import org.slf4j.{Logger, LoggerFactory}
import org.specs.Specification
import actors.Actor
import actors.Actor._
import java.util.concurrent.{TimeUnit, CyclicBarrier}
import java.util.concurrent.atomic.AtomicReference

class RabbitMQSpecs extends Specification {
  val log: Logger = LoggerFactory.getLogger(classOf[RabbitMQSpecs])

  "RabbitMQ" should {

    "handle a message produced and then consumed" in {
      val HELLO = "Hello, it is " + System.currentTimeMillis

      val converter = new StringConverter
      val cnxFactory = RabbitMQ.newDefaultConnectionFactory
      val dispatcher = new Dispatcher[String](cnxFactory, converter)
      dispatcher.start

      val barrier = new CyclicBarrier(2)
      val received = new AtomicReference[(String,String)]
      val client: Actor = actor {
        loop {
          react {
            case Dispatch(queueName:String, msg:String) =>
              received.set((queueName,msg))
              barrier.await
            case x =>
              println("Unknown received: "+x)
          }
        }
      }

      val QUEUE_NAME = "mccallum"
      dispatcher ! ConnectToQueue(QUEUE_NAME, true, false)
      dispatcher ! AddSubscriber(client)
      dispatcher ! Publish(HELLO, true)

      //
      barrier.await(300, TimeUnit.MILLISECONDS)
      received.get._1 must equalTo(QUEUE_NAME)
      received.get._2 must equalTo(HELLO)
    }
  }
}