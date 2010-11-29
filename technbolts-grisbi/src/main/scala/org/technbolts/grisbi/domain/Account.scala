package org.technbolts.grisbi.domain

import org.technbolts.grisbi.event.Event
import collection.mutable.{HashMap, ListBuffer, Set}

case class AccountId(value:String) extends Id

class Account(val id:AccountId) extends HasId[AccountId] with HasAttributes {
  private val events = ListBuffer[AccountEvent]()

  def history = events.toList

}

case class AccountGroupId(value:String) extends Id

class AccountGroup(val id:AccountGroupId) extends HasId[AccountGroupId] with HasAttributes {
  private var accountIds = Set.empty[AccountId]
}

sealed trait AccountStatus
object AccountStatus {
  case object Active extends AccountStatus
  case class  ConnectedTo(applicationId:ApplicationId) extends AccountStatus
  case class  Answering(requestId:RequestId) extends AccountStatus
  case class  Viewing(requestId:RequestId) extends AccountStatus
}

sealed trait AccountEvent extends Event {
  def accountId: AccountId
}

case class AccountCreated(accountId: AccountId) extends AccountEvent
case class AccountDeleted(accountId: AccountId) extends AccountEvent

/* update */
case class AccountGroupChanged(accountId: AccountId, oldGroupId: AccountGroupId, newGroupId: AccountGroupId) extends AccountEvent
case class AccountAttributeChanged(accountId: AccountId, attribute:Attribute) extends AccountEvent
case class AccountStatusChanged(accountId: AccountId, accountStatus: AccountStatus, mode: Mode) extends AccountEvent

