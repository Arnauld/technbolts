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

      val exchange = amqp.fanout("multicast").start
      val queue1 = amqp.queue("listener1").bindTo(amqp.fanout("multicast")).start
      queue1.subscribe { m =>
        println("[1] >"+m.contentAsString)
      }
      val queue2 = amqp.queue("listener2").bindTo(amqp.fanout("multicast")).start
        queue1.subscribe { m =>
        println("[2] >"+m.contentAsString)
      }
      exchange.publish(Message("Hey man! What's up?"))

    }

    "provide a simple way to manage Distributed Jobs" in {
      val amqp = newAMQP

      val masterNode = amqp.fanout("p.jobs").using(FallbackRoutingKey(RoutingKey("rk.jobs"))).start

      val COUNT = 3
      1.to(COUNT).map( i => {
        val workerNode = amqp.queue("jobs").bindToFanout("p.jobs").using(AutoAck(false)).initializer({
          /*
           * That tells Rabbit not to give more than one message to a worker at a time. Or, in other
           * words, don't dispatch a new message to a worker until it has processed and acknowledged
           * the previous one.
           */
          channel =>
            println("Setting QoS...")
            channel.basicQos(1)
        }).start

        val random = new Random
        workerNode.subscribe { m =>
          Thread.sleep((COUNT-i)*200)
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