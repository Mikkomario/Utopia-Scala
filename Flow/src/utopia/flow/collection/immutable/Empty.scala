package utopia.flow.collection.immutable

/**
  * An empty collection
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
object Empty extends IndexedSeq[Nothing]
{
	// IMPLEMENTED  ---------------------
	
	override def iterableFactory = OptimizedIndexedSeq
	
	override def length: Int = 0
	override def iterator: Iterator[Nothing] = Iterator.empty
	override def reverseIterator = iterator
	
	override def isEmpty = true
	override def knownSize = 0
	
	override def empty = this
	override def reverse = this
	override protected def reversed = this
	override def distinct = this
	override def sorted[B >: Nothing](implicit ord: Ordering[B]) = this
	
	override def headOption = None
	override def lastOption = None
	
	override def toString() = "Empty"
	
	override def isDefinedAt(idx: Int) = false
	override def apply(i: Int): Nothing =
		throw new IndexOutOfBoundsException("apply(Int) called on an empty collection")
	
	override def distinctBy[B](f: Nothing => B) = this
	
	override def sortWith(lt: (Nothing, Nothing) => Boolean) = this
	override def sortBy[B](f: Nothing => B)(implicit ord: Ordering[B]) = this
	
	override def filter(pred: Nothing => Boolean) = this
	override def filterNot(pred: Nothing => Boolean) = this
	
	override def take(n: Int) = this
	override def takeRight(n: Int) = this
	override def takeWhile(p: Nothing => Boolean) = this
	
	override def drop(n: Int) = this
	override def dropRight(n: Int) = this
	override def dropWhile(p: Nothing => Boolean) = this
	
	override def map[B](f: Nothing => B) = this
	override def flatMap[B](f: Nothing => IterableOnce[B]) = this
	
	override def zip[B](that: IterableOnce[B]) = this
	override def zipWithIndex = this
	
	override def foreach[U](f: Nothing => U) = ()
	
	override def forall(p: Nothing => Boolean) = true
	override def exists(p: Nothing => Boolean) = false
	override def count(p: Nothing => Boolean) = 0
	
	override def find(p: Nothing => Boolean) = None
	override def findLast(p: Nothing => Boolean) = None
	
	override def indexWhere(p: Nothing => Boolean, from: Int) = -1
	override def lastIndexWhere(p: Nothing => Boolean, end: Int) = -1
	
	override def contains[A1 >: Nothing](elem: A1) = false
	override def containsSlice[B >: Nothing](that: collection.Seq[B]) = that.isEmpty
	
	override def canEqual(that: Any) = that.isInstanceOf[IterableOnce[_]]
	override def equals(o: Any) = o match {
		case i: Iterable[_] => i.isEmpty
		case i: IterableOnce[_] => i.iterator.isEmpty
		case _ => false
	}
	override def sameElements[B >: Nothing](o: IterableOnce[B]) = o.iterator.isEmpty
	
	override def appended[B >: Nothing](elem: B) = Single(elem)
	override def prepended[B >: Nothing](elem: B) = Single(elem)
	override def appendedAll[B >: Nothing](suffix: IterableOnce[B]) = IndexedSeq.from(suffix)
	override def prependedAll[B >: Nothing](prefix: IterableOnce[B]) = IndexedSeq.from(prefix)
	
	override def padTo[B >: Nothing](len: Int, elem: B) = OptimizedIndexedSeq.fill(len)(elem)
}
