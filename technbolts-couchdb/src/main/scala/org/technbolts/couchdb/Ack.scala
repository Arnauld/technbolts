package org.technbolts.couchdb

import org.codehaus.jackson.JsonNode

object AckImplicits {
  implicit def jsonNode2Ack(underlying:JsonNode):Ack = {
    underlying.get("ok") match {
      case null => {
        underlying.get("error") match {
          case null => // oops we're in trouble
            throw new CouchDBException ("Node is not an Ack: " + underlying)
          case node => Error(node.getTextValue, underlying.get("reason").getTextValue)
        }
      }
      case node => Ok
    }
  }
}

sealed abstract class Ack {
  def isOk:Boolean
  def isError = !isOk
}
case object Ok extends Ack {
  override def isOk = true
}
case class Error(code:String, reason:String) extends Ack {
  override def isOk = false
}