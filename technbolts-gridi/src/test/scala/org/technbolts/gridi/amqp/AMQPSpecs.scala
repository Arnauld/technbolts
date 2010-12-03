package org.technbolts.gridi.amqp

import org.specs.Specification
import org.technbolts.gridi.amqp.temp.Message

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
  }
}