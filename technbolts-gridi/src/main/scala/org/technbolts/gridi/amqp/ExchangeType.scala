package org.technbolts.gridi.amqp

sealed abstract class ExchangeType(val key:String)

/**
 * Direct exchanges can be simulated by using topic exchanges without wildcards, and fanout exchanges can be simulated
 * by binding to topic exchanges with a full wildcard pattern.
 *
 * The different kinds of exchange exist mainly for clarity's sake: it's easier for an observer looking at a system to
 * see the intent behind the way it's configured. Secondarily, it opens up opportunities for optimising the less-general
 * kinds of exchange.
 */
object ExchangeType {

  /**
   * Any message sent to a direct exchange will be delivered to all queues bound to
   * it where the message routing key matches the binding routing key.
   * 
   * The direct exchange type works as follows:
   * <ol>
   *   <li> A message queue binds to the exchange using a routing key, K.</li>
   *   <li> A publisher sends the exchange a message with the routing key R.</li>
   *   <li> The message is passed to the message queue if K = R.</li>
   * </ol>
   */
  case object Direct extends ExchangeType("direct")

  /**
   * Any message sent to a fanout exchange will be delivered to all queues bound to it.
   *
   * The fanout exchange type works as follows:
   * <ol>
   *   <li>A message queue binds to the exchange with no arguments.</li>
   *   <li>A publisher sends the exchange a message.</li>
   *   <li>The message is passed to the message queue unconditionally.</li>
   * </ol>
   */
  case object Fanout extends ExchangeType("fanout")

  /**
   * The topic exchange type works as follows:
   * <ol>
   *   <li>A message queue binds to the exchange using a routing pattern, P.</li>
   *   <li>A publisher sends the exchange a message with the routing key R.</li>
   *   <li>The message is passed to the message queue if R matches P.</li>
   * </ol>
   */
  case object Topic extends ExchangeType("topic")
}
