package org.technbolts.grisbi.domain

abstract sealed class Mode
object Mode {
  case object Set extends Mode
  case object Unset extends Mode
  case object Add extends Mode
  case object Remove extends Mode
}


