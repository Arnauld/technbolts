package org.technbolts.util

import java.util.concurrent.ThreadFactory
import java.util.concurrent.atomic.AtomicInteger

object Threads {
  def newDaemonThreadFactory(poolName:String) = new DaemonThreadFactory(newNamedThreadFactory(poolName))
  def newDaemonThreadFactory(poolName:String,  threadGroup:ThreadGroup) = new DaemonThreadFactory(newNamedThreadFactory(poolName, threadGroup))
  def newNamedThreadFactory (poolName:String) = new NamedThreadFactory (poolName+"-thread-%d")
  def newNamedThreadFactory (poolName:String,  threadGroup:ThreadGroup) = new NamedThreadFactory (poolName+"-thread-%d") {
    override def getThreadGroup = threadGroup
  }
}

class NamedThreadFactory(baseNamePattern:String) extends ThreadFactory {

  private val idGen = new AtomicInteger
  def getThreadGroup = Thread.currentThread.getThreadGroup
  def newThread(r: Runnable): Thread = new Thread(getThreadGroup, r, baseNamePattern.format(idGen.incrementAndGet))
}

class DaemonThreadFactory(underlying: ThreadFactory) extends ThreadFactory {
    def newThread(r: Runnable) = {
        val t = underlying.newThread(r)
        t.setDaemon(true)
        t
    }
}