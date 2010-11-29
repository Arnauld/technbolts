package org.technbolts.grisbi.domain

import org.technbolts.grisbi.event.Event

case class CustomerId(value:String) extends Id

class Customer(val id:CustomerId) extends HasId[CustomerId] with HasAttributes


sealed trait CustomerEvent extends Event {
  def customerId: CustomerId
}

case class CustomerCreated(customerId: CustomerId) extends CustomerEvent
case class CustomerDeleted(customerId: CustomerId) extends CustomerEvent

/* update */
case class CustomerAttributeChanged(customerId: CustomerId, attribute:Attribute) extends CustomerEvent

