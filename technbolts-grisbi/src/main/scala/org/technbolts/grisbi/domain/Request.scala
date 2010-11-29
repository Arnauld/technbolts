package org.technbolts.grisbi.domain

import collection.mutable.ListBuffer

case class RequestId(value:String) extends Id

class Request(val id:RequestId, customerId:CustomerId) extends HasId[RequestId] with HasAttributes {
  private val events = ListBuffer[RequestEvent]()

  def history = events.toList
}

sealed trait RequestEvent

