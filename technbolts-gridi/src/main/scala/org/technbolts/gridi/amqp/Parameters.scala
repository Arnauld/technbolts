package org.technbolts.gridi.amqp

trait Param { def sym:Symbol }
sealed trait ExchangeParam extends Param
sealed trait QueueParam extends Param

abstract class ParamImpl(val sym: Symbol) extends Param
object Param {
  val DefaultDurable = Durable(false)
  val DefaultExclusive = Exclusive(false)
  val DefaultAutoDelete = AutoDelete(false)
  val DefaultArguments = Arguments(Nil)
  val DefaultAutoAck = AutoAck(true)
  /**
   * Queues and Exchanges can be marked as durable.
   * All this means is that if the server restarts, the queues will still be there.
   * Individual messages have to be sent in delivery mode 'persistent' for them to survive a server restart.
   */
  case class Durable(durable: Boolean) extends ParamImpl('durable) with ExchangeParam with QueueParam

  /**
   * Exclusive true if we are declaring an exclusive queue (restricted to this connection)
   */
  case class Exclusive(exclusive: Boolean) extends ParamImpl('durable) with QueueParam

  /**
   * autoDelete true if we are declaring an autodelete queue (server will delete it when no longer in use).
   */
  case class AutoDelete(autoDelete: Boolean) extends ParamImpl('autoDelete) with ExchangeParam with QueueParam
  case class Arguments(values: List[(String, Any)]) extends ParamImpl('arguments) with ExchangeParam with QueueParam
  /**
   * Default routing key used when publishing a message.
   * Routing key used when publishing a message that has no routing key defined.
   */
  case class FallbackRoutingKey(key:RoutingKey) extends ParamImpl('fallbackRoutingKey) with ExchangeParam
  case class AutoAck(autoAck: Boolean) extends ParamImpl('autoAck) with QueueParam
}