package org.technbolts.ref

import org.specs.Specification
import org.slf4j.{LoggerFactory, Logger}

import Transaction._

class RefSpecs extends Specification {
  val log: Logger = LoggerFactory.getLogger(classOf[RefSpecs])

  "implicit context" should {

    "be bound automatically" in {
      val ref = new Ref[String](null)
      ref.get mustBe null
      ref := "zog"
      ref.get must equalTo("zog")
    }
  }

  "Option type in implicit context" should {
    "correctly be handled with None as initial value" in {
      val ref = new Ref[Option[String]] (None)
      ref.get must equalTo(None)
      ref := Some("grunt")
      ref.get must equalTo(Some("grunt"))
      ref := None
      ref.get must equalTo(None)
    }
    "correctly be handled with Some as initial value" in {
      val ref = new Ref[Option[String]] (Some("ogre"))
      ref.get must equalTo(Some("ogre"))
      ref := Some("grunt")
      ref.get must equalTo(Some("grunt"))
      ref := None
      ref.get must equalTo(None)
    }
  }

  "Transaction" should {
    "work for simple case" in {
      val ref = new Ref[String]("Probe")
      within(new Transaction, { implicit tx =>
        ref.get must equalTo("Probe")
        ref := "Templar"
        tx.values.values.find(_ =="Templar").isDefined mustBe true
        ref.get must equalTo("Templar")
      })
      ref.get must equalTo("Probe")
    }

    "can be committed" in {
      val ref = new Ref[String]("Probe")
      val txn = new Transaction
      within(txn, { implicit tx =>
        ref := "Templar"
      })
      ref.get must equalTo("Probe")
      txn.commit
      ref.get must equalTo("Templar")
    }

    "keep the original value when commited without change" in {
      val ref = new Ref[String]("Zeelot")
      val txn = new Transaction
      within(txn, { implicit tx =>
        // no op
        val b = 1+2
      })
      txn.commit
      ref.get must equalTo("Zeelot")
    }

    "provide a change log" in {
      val ref = new Ref[String]("Zeelot")
      val txn = new Transaction
      within(txn, { implicit tx =>
        ref := "Dragoon"
      })
      val log = new ChangeLogRecorder
      txn.commit(log)
      log.getChanges.size must equalTo(1)
    }
  }

}