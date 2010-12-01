package org.technbolts.grisbi.domain

import collection.mutable.HashMap


case class AttributeId(value:String) extends Id

case class Attribute (val id:AttributeId, val value:Any) extends HasId[AttributeId]

trait HasAttributes {

  protected val attrs = new HashMap[AttributeId,Attribute]

  def attributes:Map[AttributeId,Attribute] = attrs.toMap

  def getAttributeValue(id:AttributeId):Option[Any] = attrs.get(id) match {
    case Some(Attribute(_,value)) => Some(value)
    case None => None
  }
}

trait HasMutableAttributes { self:HasAttributes =>
  def setAttribute(id:AttributeId, value:Any)
}