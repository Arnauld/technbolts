package org.technbolts.gridi.amqp

import collection.mutable.{ListBuffer, HashMap}

trait MQ {
  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Producer part
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */
  def exchange(exchangeName:ExchangeName, exchangeType:ExchangeType):ExchangeBuilder

  /* ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   * Consumer part
   * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
   */
  def queue(queueName:QueueName):QueueBuilder
}

trait ExchangeBuilder {
  var isDurable = false
  /**
   * Queues and Exchanges can be marked as durable.
   * All this means is that if the server restarts, the queues will still be there.
   * Individual messages have to be sent in delivery mode 'persistent' for them to survive a server restart.
   */
  def durable(durable:Boolean):ExchangeBuilder = {
    this.isDurable = durable;
    this
  }

  var defaultRoutingKey:Option[RoutingKey] = None
  /**
   * Default routing key used when publishing a message.
   * 
   * @param routingKey routing key used when publishing a message that has no routing key defined.
   */
  def usingDefault(routingKey:RoutingKey) = {
    defaultRoutingKey = Some(routingKey)
    this
  }

}

/**
 * 
 */
trait QueueBuilder {

  /**
   * Build a Queue and bind it according to the current settings
   */
  def build:Queue

  val routingKeys = ListBuffer[RoutingKey]()
  /**
   *
   * <strong>This may lead to unknown behavior if not used on a direct based exchange</strong>
   * @see RoutingKey
   */
  def using(routingKey:RoutingKey):QueueBuilder = {
    this.routingKeys+=routingKey
  }

  val routingPatterns = ListBuffer[RoutingPattern]()

  /**
   *
   * <strong>This may lead to unknown behavior if not used on a topic based exchange</strong>
   * @see RoutingPattern
   */
  def using(routingPattern:RoutingPattern):QueueBuilder = {
    this.routingPatterns+=routingPattern
  }

  var exchangeName:Option[ExchangeName] = None
  def bindTo(exchangeName:ExchangeName):QueueBuilder = {
    this.exchangeName = Some(exchangeName)
    this
  }

  var isDurable = false

  /**
   * Queues and Exchanges can be marked as durable.
   * All this means is that if the server restarts, the queues will still be there.
   *
   * <strong>Durable queues can only bind to durable exchanges</strong>.
   */
  def durable(durable:Boolean):QueueBuilder = {
    this.isDurable = durable
    this
  }

  var isExclusive = false
  def exclusive(exclusive:Boolean):QueueBuilder = {
    this.isExclusive = exclusive
    this
  }

  var isAutoDelete = false
  def autoDelete(autoDelete:Boolean):QueueBuilder = {
    this.isAutoDelete = autoDelete
    this
  }

  val arguments = new HashMap[String,Any]
  def argument(arg:(String,Any)):QueueBuilder = {
    this.arguments += arg
    this
  }

  def arguments(arg1:(String,Any), arg2:(String,Any), args:(String,Any)*):QueueBuilder = {
    this.arguments += arg1
    this.arguments += arg2
    args.foreach( this.arguments += _ ) 
    this
  }
}