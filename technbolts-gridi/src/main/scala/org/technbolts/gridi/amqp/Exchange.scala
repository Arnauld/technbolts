package org.technbolts.gridi.amqp

trait Exchange {
  def exchangeName:String

  /**
   * Publish a message in the exchange.<p/>
   */
  def publish(message: Message): Unit
}

