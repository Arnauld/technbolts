package org.technbolts.util

import org.specs.Specification

class HashFunctionSpecs extends Specification {
  import HashFunction._

  "HashFunction" should {
    val longKey = "the easter island statue memorial tabernacle choir presents"

    "FNV1-32" in {
      HashFunction.fnv1_32.hash("") mustEqual 2166136261L
      HashFunction.fnv1_32.hash("cat") mustEqual 983016379L
      HashFunction.fnv1_32.hash(longKey) mustEqual 2223726839L
    }

    "FNV1A-32" in {
      HashFunction.fnv1a_32.hash("") mustEqual 2166136261L
      HashFunction.fnv1a_32.hash("cat") mustEqual 108289031L
      HashFunction.fnv1a_32.hash(longKey) mustEqual 1968151335L
    }

    "FNV1-64(1)" in {
      HashFunction.fnv1_64.hash("") mustEqual 0x84222325L
      HashFunction.fnv1_64.hash(" ") mustEqual 0x8601b7ffL
    }
    
    "FNV1-64(2)" in {
      HashFunction.fnv1_64.hash("cat") mustEqual 1806270427L
      HashFunction.fnv1_64.hash(longKey) mustEqual 3588698999L
      HashFunction.fnv1_64.hash("hello world!") mustEqual 0xb97b86bcL
      HashFunction.fnv1_64.hash("Lorem ipsum dolor sit amet, consectetuer adipiscing elit.") mustEqual 0xe87c054aL
    }

    "FNV1-64(3)" in {
      HashFunction.fnv1_64.hash("wd:com.google") mustEqual 0x071b08f8L
      HashFunction.fnv1_64.hash("wd:com.google ") mustEqual 0x12f03d48L
    }

    "FNV1A-64" in {
      HashFunction.fnv1a_64.hash("") mustEqual 2216829733L
      HashFunction.fnv1a_64.hash("cat") mustEqual 216310567L
      HashFunction.fnv1a_64.hash(longKey) mustEqual 2969891175L
    }

    "ketama" in {
      /*
      HashFunction.KETAMA.hashKey("".getBytes) mustEqual 3649838548L
      HashFunction.KETAMA.hashKey("\uffff".getBytes("utf-8")) mustEqual 844455094L
      HashFunction.KETAMA.hashKey("cat".getBytes) mustEqual 1156741072L
      HashFunction.KETAMA.hashKey(longKey.getBytes) mustEqual 3103958980L
      */
    }
  }
}