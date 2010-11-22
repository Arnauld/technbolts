package org.technbolts.gridi

import org.slf4j.{Logger, LoggerFactory}
import org.specs.Specification
import actors.Actor
import actors.Actor._
import java.util.concurrent.CyclicBarrier

class RabbitMQSpecs extends Specification {
  val log: Logger = LoggerFactory.getLogger(classOf[RabbitMQSpecs])

  "RabbitMQ" should {

    "handle a message produced and then consumed" in {
      val HELLO = "Hello, it is " + System.currentTimeMillis

      val converter = new StringConverter
      val cnxFactory = RabbitMQ.newConnectionFactory
      val dispatcher = new Dispatcher[String](cnxFactory, converter)
      dispatcher.start

      val barrier = new CyclicBarrier(2)
      val client: Actor = actor {
        loop {
          react {
            case Dispatch(msg) =>
              barrier.await
              println("~~>" + msg)
            case x =>
              println("~~>" + x)
          }
        }
      }
      dispatcher ! ConnectToQueue("queue", true, false)
      dispatcher ! AddSubscriber(client)
      dispatcher ! Publish(HELLO, true)
      barrier.await
    }
  }
}