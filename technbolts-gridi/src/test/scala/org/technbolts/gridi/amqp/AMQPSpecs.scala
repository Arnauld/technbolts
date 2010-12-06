package org.technbolts.gridi.amqp

import org.specs.Specification
import org.technbolts.gridi.amqp.Param.{FallbackRoutingKey, AutoAck}
import util.Random

class AMQPSpecs extends Specification {

  def newAMQP:AMQP = {
    import AMQP.ConnectParam._
    //def amqp = AMQP.connect('host -> "localhost", 'user->"guest", 'pass->"guest", 'vhost->"/")
    AMQP.connect(Host("localhost"))
  }

  "AMQP" should {
    "establish a connection with the broker" in {
      val amqp = newAMQP

      val ex_name = "multicast"
      val exchange = amqp.fanout(ex_name).start

      val queue1 = amqp.queue("listener1").bindTo(ex_name).start
      queue1.subscribe { m =>
        println("[1] >"+m.contentAsString)
      }
      val queue2 = amqp.queue("listener2").bindTo(ex_name).start
        queue1.subscribe { m =>
        println("[2] >"+m.contentAsString)
      }
      exchange.publish(Message("Hey man! What's up?"))

    }

    "provide a simple way to manage Distributed Jobs" in {
      val amqp = newAMQP

      val masterNode = amqp.fanout("p.jobs").start

      val COUNT = 3
      1.to(COUNT).map( i => {
        /*
         * All workers share the same queue thus the jobs are distributed among them and not
         * to all of them, as it would be in case of multiple queues on a fanout
         *
         * :autoAck->false : Let the ack be done once the worker is done, ensuring the message
         *                  has been completely be processed before removing it from the queue
         * :qos->1 : That tells Rabbit not to give more than one message to a worker at a time.
         *           Or, in other words, don't dispatch a new message to a worker until it has
         *           processed and acknowledged the previous one.
         */
        val workerNode = amqp.queue("jobs").bindTo("p.jobs")
                                .autoAck(false)
                                .qualityOfService(1)
                                .start

        workerNode.subscribe { m =>
          Thread.sleep((COUNT-i)*20)
          println("["+i+"] " + m.contentAsString)
        }
      })

      1.to(10).foreach { i =>
        println("> publishing #"+i)
        masterNode.publish(Message("Job #"+i))
      }

      println("Press key to stop")
      System.in.read
    }
  }

}