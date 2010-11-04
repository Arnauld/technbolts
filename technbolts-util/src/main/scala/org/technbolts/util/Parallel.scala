package org.technbolts.util

import java.util.concurrent.{TimeUnit, Callable, Executors}
import collection.mutable.{Buffer, ListBuffer}

object Parallel {
  def availableProcessors = Runtime.getRuntime.availableProcessors
  def suggestedProcessors = NumberUtils.clamp(2,16)(availableProcessors)

  lazy val sharedWorkers = new ThreadPoolWorkers(suggestedProcessors)

  def find[A](predicate:(A)=>Boolean, items:List[A]) = parallelList(items).find(predicate)(sharedWorkers)

  def parallelList[A](items:List[A]) = new ParallelList[A](items)
}

trait Workers {
  def invokeAll[A](jobs:List[Callable[A]]):Buffer[A]
}

class ProxyWorkers(underlying:Workers) extends Workers {
  def invokeAll[A](jobs:List[Callable[A]]) = underlying.invokeAll(jobs)
}

class ThreadPoolWorkers(parallelism:Int) extends Workers {
  def timeout     =  5
  def timeoutUnit =  TimeUnit.SECONDS

  val pool = Executors.newFixedThreadPool(parallelism, Threads.newNamedThreadFactory("Parallel"))
  override def invokeAll[A](jobs:List[Callable[A]]):Buffer[A] = {
    import collection.JavaConversions._
    pool.invokeAll(jobs).map(_.get(timeout, timeoutUnit) )
  }
}

class ParallelList[A](items:List[A]) {

  implicit object workers extends ProxyWorkers(Parallel.sharedWorkers)

  def find(predicate:(A)=>Boolean)(implicit workers:Workers) = {
    val works = items.grouped(25).map(list => new Callable[List[A]] {
        override def call = list.filter(predicate)
      }).toList
    val result = ListBuffer[A]()
    workers.invokeAll(works).foldLeft(result)(_++_)
  }
}