package org.technbolts.gridi.amqp

/**
 * From the AMQP 0-9-1 spec:
 *
 * The topic exchange type works as follows:
 * <ol>
 * <li> A message queue binds to the exchange using a routing pattern, P.</li>
 * <li> A publisher sends the exchange a message with the routing key R. </li>
 * <li> The message is passed to the message queue if R matches P.</li>
 * </ol>
 * <p>
 * The routing key used for a topic exchange MUST consist of <em>zero or more <b>words</b> delimited by dots</em>.
 * Each <b>word</b> may contain the letters A-Z and a-z and digits 0-9.
 * The routing pattern follows the same rules as the routing key with the addition that * matches a single word, and # matches zero
 * or more words.
 * </p>
 * <p>
 * Thus the routing pattern <code>*.stock.#</code> matches the routing keys <code>usd.stock</code>
 * and <code>eur.stock.db</code> but not <code>stock.nasdaq</code>.
 * </p>
 */
case class RoutingPattern(pattern:String) {
  if(!pattern.matches("""[a-zA-Z0-9*#]+(.[a-zA-Z0-9*#]+)*"""))
    throw new InvalidRoutingPattern(pattern)
}

class InvalidRoutingPattern(message:String) extends Exception(message)

/**
 * The direct exchange type works as follows:
 * <ol>
 *   <li> A message queue binds to the exchange using a routing key, K.</li>
 *   <li> A publisher sends the exchange a message with the routing key R.</li>
 *   <li> The message is passed to the message queue if K = R.</li>
 * </ol>
 */
case class RoutingKey(key:String)