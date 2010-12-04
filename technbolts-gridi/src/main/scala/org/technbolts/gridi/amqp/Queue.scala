package org.technbolts.gridi.amqp

trait Queue {
  def queueName:String
  def subscribe(callback: (Message) => Unit): Unit
}