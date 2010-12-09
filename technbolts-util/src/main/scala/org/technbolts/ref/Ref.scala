package org.technbolts.ref

class Ref[T](v: T) {

  private[ref] var value:T = v

  def get(implicit c: Context):T = if(c.contains(this))
                                     c.retrieve(this)
                                   else
                                     value

  def :=(v: T)(implicit c: Context) {
    c.store(this, v)
  }

  private[ref] def commit(tx:Transaction, changeLog:ChangeLog):Unit = {
    val newValue = get(tx)
    val oldValue = value
    value = newValue
    if(!areEqual(newValue, oldValue))
      changeLog.refChanged(this, oldValue, newValue)
  }

  protected def areEqual(v1:T, v2:T):Boolean = { v1 ==v2 }
}

class NamedRef[T](val name:String, v: T) extends Ref[T](v)

object Implicits {
  implicit def refToValue[T](ref: Ref[T])(implicit c: Context) = {
    ref.get(c)
  }
}

abstract class Context private[ref] () {
  private[ref] def contains[T](ref: Ref[T]): Boolean
  private[ref] def retrieve[T](ref: Ref[T]): T
  private[ref] def store[T](ref: Ref[T], v: T):Unit
}

object Context {
  implicit object LiveContext extends Context {
    private[ref] def contains[T](ref: Ref[T]) = false
    private[ref] def retrieve[T](ref: Ref[T]) = ref.value
    private[ref] def store[T](ref: Ref[T], v: T):Unit = { ref.value = v }
  }
}

object Transaction {
  def within[T](tx:Transaction, f: (Transaction)=>T):T = f(tx)
  def atomic[T](f: =>T)(implicit changeLog:ChangeLog) = within(new Transaction, { tx =>
    f
    tx.commit(changeLog)
  })
}

case class Change(ref:Ref[Any], oldValue:Any, newValue:Any)

class ChangeLogRecorder extends ChangeLog {
  private var changes:List[Change] = Nil
  def refChanged[T](ref: Ref[T], oldValue: T, newValue: T) =
    changes ::= Change(ref.asInstanceOf[Ref[Any]], oldValue.asInstanceOf[Any], newValue.asInstanceOf[Any])
  def getChanges = changes.toList
}

trait ChangeLog {
  def refChanged[T](ref:Ref[T], oldValue:T, newValue:T):Unit
}

object ChangeLog {
  implicit object LiveChangeLog extends ChangeLog {
    def refChanged[T](ref: Ref[T], oldValue: T, newValue: T) = {
      // no op
      // println(""+ref+": "+oldValue+"~~>"+newValue)
    }
  }
}

class Transaction extends Context {
  private[ref] val values = scala.collection.mutable.Map[Ref[Any], Any]()

  private[ref] def contains[T](ref: Ref[T]):Boolean = {
    val castRef = ref.asInstanceOf[Ref[Any]]
    values.contains(castRef)
  }

  private[ref] def retrieve[T](ref: Ref[T]): T = {
    val castRef = ref.asInstanceOf[Ref[Any]]
    values.get(castRef).get.asInstanceOf[T]
  }

  private[ref] def store[T](ref: Ref[T], v: T):Unit = {
    val castRef = ref.asInstanceOf[Ref[Any]]
    val prev = values.put(castRef, v)
  }

  def commit(implicit changeLog:ChangeLog):Unit = {
    values.foreach { e => e._1.commit(this, changeLog) }
  }
}