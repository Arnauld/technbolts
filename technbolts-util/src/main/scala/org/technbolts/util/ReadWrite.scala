package org.technbolts.util

import java.util.concurrent.locks.{Lock, ReentrantReadWriteLock}

object LockSupport {
  def withinLock[T](lock:Lock)(f: =>T):T = try {
    lock.lock
    f
  }finally lock.unlock
}

trait ReadWrite {
  private val rwLock = new ReentrantReadWriteLock

  import LockSupport._

  def read[T](f: =>T) = withinLock(rwLock.readLock)(f)

  def write[T](f: =>T):T = withinLock(rwLock.writeLock)(f)
}