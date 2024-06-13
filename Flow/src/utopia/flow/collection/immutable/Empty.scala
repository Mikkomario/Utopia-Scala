package utopia.flow.collection.immutable

/**
  * An empty collection
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
object Empty extends IsEmpty

/**
  * Common trait for concrete empty collections
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
trait IsEmpty extends IndexedSeq[Nothing] with EmptyOps[IndexedSeq, IndexedSeq[Nothing], Single, IsEmpty]
{
	// IMPLEMENTED  ---------------------
	
	override protected def self = this
	override protected def reversed = this
	override def view = EmptyView
	
	override def iterableFactory = OptimizedIndexedSeq
	
	override def toString() = "Empty"
	
	override protected def wrapSingle[B](value: => B): Single[B] = Single(value)
	
	override def canEqual(that: Any) = that.isInstanceOf[IterableOnce[_]]
	override def equals(o: Any) = super[EmptyOps].equals(o)
	
	override def appendedAll[B >: Nothing](suffix: IterableOnce[B]) = OptimizedIndexedSeq.from(suffix)
	override def prependedAll[B >: Nothing](prefix: IterableOnce[B]) = OptimizedIndexedSeq.from(prefix)
	
	override def padTo[B >: Nothing](len: Int, elem: B) = OptimizedIndexedSeq.fill(len)(elem)
}
