package org.technbolts.couchdb

import org.specs.Specification

class DatabaseSpecs extends Specification with CouchDBSettings {
  "Database name must be valid" in {
    Database.isNameValid("Technbolts") must equalTo(false)
    Database.isNameValid("tEchnbOlts") must equalTo(false)
    Database.isNameValid("7echnbolts") must equalTo(false)
    Database.isNameValid("t3chn60l7s") must equalTo(true)
    Database.isNameValid("tech-n-bolts") must equalTo(true)
  }

  "Database" should {
    "provides a way to check if it exists" in {
      val couchDB = newCouchDB
      val mcCallum = new Database(couchDB, "mc_callum")
      mcCallum.exists must beEqualTo(true)
      val weird = new Database(couchDB, "w31rd")
      weird.exists must beEqualTo(false)
    }

    "cannot be created if it already exists" in {
        val couchDB = newCouchDB
        val mcCallum = new Database(couchDB, "mc_callum")
        mcCallum.create must throwA[DatabaseAlreadyExistsException]
    }
  }
}