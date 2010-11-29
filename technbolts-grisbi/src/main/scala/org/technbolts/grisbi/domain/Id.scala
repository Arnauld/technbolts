package org.technbolts.grisbi.domain

trait Id {
  def value:String
}

trait HasId[T<:Id] {
  def id:T
}