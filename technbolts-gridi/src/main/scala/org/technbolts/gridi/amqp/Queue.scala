package org.technbolts.gridi.amqp

import collection.mutable.{ListBuffer, HashMap}

case class QueueName(queueName:String)

trait Queue {
  /**
   * When subscribing to a queue, you can enable manual acknowledgements.
   * If a consumer disconnects, all unacknowledged messages will be returned to the queue.
   */
  def subscribe(consumer:QueueConsumer, manualAck:Boolean)
}

trait QueueConsumer
