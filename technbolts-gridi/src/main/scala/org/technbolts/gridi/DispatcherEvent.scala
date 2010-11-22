package org.technbolts.gridi

import actors.Actor
import com.rabbitmq.client.{Envelope, BasicProperties}

sealed trait DispatcherEvent

case class AddSubscriber(subscriber: Actor) extends DispatcherEvent
case class RemoveSubscriber(subscriber: Actor) extends DispatcherEvent
case class Dispatch[T](queueName:String, message: T) extends DispatcherEvent
case class Publish[T](message: T, persistent: Boolean) extends DispatcherEvent
case class ConnectToQueue(queueName: String, durable: Boolean, noAck: Boolean) extends DispatcherEvent
case class Delivery(consumerTag: String, envelope: Envelope, properties: BasicProperties, body: Array[Byte]) extends DispatcherEvent
