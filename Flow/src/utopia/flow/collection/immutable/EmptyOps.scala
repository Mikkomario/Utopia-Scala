package utopia.flow.collection.immutable

import scala.collection.immutable.IndexedSeqOps

/**
  * Common trait for collections that are always empty
  * @tparam CC Collection constructor used for variable length, generic type content
  * @tparam C Type of collection with same type of content (Nothing) and variable length
  * @tparam Repr Type of this collection instance
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
// NB: IntelliJ shows errors here which are not real errors
trait EmptyOps[+CC[+_], +C, +S[+X] <: CC[X], +Repr <: CC[Nothing] with C] extends IndexedSeqOps[Nothing, CC, C]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return This specific collection instance
	  */
	protected def self: Repr
	
	/**
	  * @param value Value to wrap (call-by-name)
	  * @tparam B Type of the wrapped value
	  * @return A collection wrapping that singular value
	  */
	protected def wrapSingle[B](value: => B): S[B]
	
	
	// IMPLEMENTED  ---------------------
	
	override def length: Int = 0
	override def iterator: Iterator[Nothing] = Iterator.empty
	override def reverseIterator = iterator
	
	override def isEmpty = true
	override def knownSize = 0
	
	override def empty = self
	override def reverse = self
	override def distinct = self
	override def sorted[B >: Nothing](implicit ord: Ordering[B]) = self
	
	override def headOption = None
	override def lastOption = None
	
	override def isDefinedAt(idx: Int) = false
	override def apply(i: Int): Nothing =
		throw new IndexOutOfBoundsException("apply(Int) called on an empty collection")
	
	override def distinctBy[B](f: Nothing => B) = self
	
	override def sortWith(lt: (Nothing, Nothing) => Boolean) = self
	override def sortBy[B](f: Nothing => B)(implicit ord: Ordering[B]) = self
	
	override def filter(pred: Nothing => Boolean) = self
	override def filterNot(pred: Nothing => Boolean) = self
	
	override def take(n: Int) = self
	override def takeRight(n: Int) = self
	override def takeWhile(p: Nothing => Boolean) = self
	
	override def drop(n: Int) = self
	override def dropRight(n: Int) = self
	override def dropWhile(p: Nothing => Boolean) = self
	
	override def slice(from: Int, until: Int) = self
	
	override def map[B](f: Nothing => B) = self
	override def flatMap[B](f: Nothing => IterableOnce[B]) = self
	
	override def zip[B](that: IterableOnce[B]) = self
	override def zipWithIndex = self
	
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
	
	override def equals(o: Any) = o match {
		case i: Iterable[_] => i.isEmpty
		case i: IterableOnce[_] => i.iterator.isEmpty
		case _ => false
	}
	override def sameElements[B >: Nothing](o: IterableOnce[B]) = o.iterator.isEmpty
	
	override def appended[B >: Nothing](elem: B) = wrapSingle(elem)
	override def prepended[B >: Nothing](elem: B) = wrapSingle(elem)
}
