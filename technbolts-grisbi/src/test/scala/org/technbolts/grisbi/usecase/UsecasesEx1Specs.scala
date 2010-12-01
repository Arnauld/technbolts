package org.technbolts.grisbi.usecase

import org.specs.Specification
import org.slf4j.{Logger, LoggerFactory}
import org.technbolts.grisbi.domain.{CustomerId, Customer}

class UsecasesEx1Specs extends Specification {
  val log: Logger = LoggerFactory.getLogger(classOf[UsecasesEx1Specs])

  "Scenario 1" should {

    "A customer can be created in the system" in {
      val customer = new Customer(CustomerId(1))
    }
  }
}