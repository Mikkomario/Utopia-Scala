package utopia.flow.collection.immutable

import scala.collection.immutable.IndexedSeqOps

/**
  * Common trait for collections and views which contain only a single item
  * @tparam A Type of wrapped values
  * @tparam CC Generic collection constructor to use (variable length, variable content type (A))
  * @tparam C Type of same underlying type (A) collections with variable length
  * @tparam S Type of generic (mapped) collections of length 1
  * @tparam P Type of generic (mapped) collections of length 2
  * @tparam Repr Type of this specific collection (length 1, type A contents)
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
// NB: IntelliJ shows a lot of errors for this class, even though it builds all right
trait SingleOps[+A, +CC[+_], +C, +E <: CC[Nothing] with C, +S[+X] <: CC[X] with utopia.flow.view.immutable.View[X], P[+X] <: CC[X], +Repr <: S[A] with C]
	extends IndexedSeqOps[A, CC, C] with utopia.flow.view.immutable.View[A]
{
	// ABSTRACT --------------------------------
	
	/**
	  * @return This collection instance
	  */
	protected def self: Repr
	
	/**
	  * @return An empty copy of this collection instance with the same underlying value type
	  */
	protected def _empty: E
	
	/**
	  * @param newValue New singular value to wrap
	  * @tparam B Type of the wrapped value
	  * @return Copy of this collection wrapping the specified value instead of the current value
	  */
	protected def wrap[B](newValue: => B): S[B]
	/**
	  * @param first First value to wrap
	  * @param second Second value to wrap
	  * @tparam B Type of the wrapped values
	  * @return A collection that wraps the two specified values
	  */
	protected def wrapTwo[B](first: => B, second: => B): P[B]
	
	
	// IMPLEMENTED  ----------------------------
	
	override def length = 1
	override def knownSize = 1
	override def isEmpty = false
	
	override def head = value
	override def headOption = Some(value)
	override def last = value
	override def lastOption = Some(value)
	
	override def min[B >: A](implicit ord: Ordering[B]) = value
	override def max[B >: A](implicit ord: Ordering[B]) = value
	override def minOption[B >: A](implicit ord: Ordering[B]) = Some(value)
	override def maxOption[B >: A](implicit ord: Ordering[B]) = Some(value)
	
	override def iterator = Iterator.single(value)
	override def valueIterator = iterator
	override def reverseIterator = iterator
	override def tail = _empty
	
	override def empty = _empty
	override def reverse = self
	override def distinct = self
	
	override def zipWithIndex = wrap(value -> 0)
	
	override def isDefinedAt(idx: Int) = idx == 0
	override def apply(v1: Int) = value
	
	override def distinctBy[B](f: A => B) = self
	override def sorted[B >: A](implicit ord: Ordering[B]) = self
	override def sortWith(lt: (A, A) => Boolean) = self
	override def sortBy[B](f: A => B)(implicit ord: Ordering[B]) = self
	
	override def contains[A1 >: A](elem: A1) = value == elem
	
	override def find(p: A => Boolean) = Some(value).filter(p)
	override def findLast(p: A => Boolean) = find(p)
	override def indexWhere(p: A => Boolean, from: Int) = if (from > 0 || !p(value)) -1 else 0
	
	override def maxBy[B](f: A => B)(implicit ord: Ordering[B]) = value
	override def maxByOption[B](f: A => B)(implicit ord: Ordering[B]) = Some(value)
	override def minBy[B](f: A => B)(implicit ord: Ordering[B]) = value
	override def minByOption[B](f: A => B)(implicit ord: Ordering[B]) = Some(value)
	
	override def foreach[U](f: A => U) = f(value)
	override def forall(p: A => Boolean) = p(value)
	override def exists(p: A => Boolean) = p(value)
	override def count(p: A => Boolean) = if (p(value)) 1 else 0
	
	override def foldLeft[B](z: B)(op: (B, A) => B) = op(z, value)
	override def foldRight[B](z: B)(op: (A, B) => B) = op(value, z)
	
	override def reduce[B >: A](op: (B, B) => B) = value
	override def reduceOption[B >: A](op: (B, B) => B) = Some(value)
	override def reduceLeft[B >: A](op: (B, A) => B) = value
	override def reduceRight[B >: A](op: (A, B) => B) = value
	override def reduceLeftOption[B >: A](op: (B, A) => B) = Some(value)
	override def reduceRightOption[B >: A](op: (A, B) => B) = Some(value)
	
	override def filter(pred: A => Boolean) = if (pred(value)) self else _empty
	override def filterNot(pred: A => Boolean) = if (pred(value)) _empty else self
	
	override def zip[B](that: IterableOnce[B]) = that.iterator.nextOption() match {
		case Some(other) => wrap(value -> other)
		case None => _empty
	}
	
	override def map[B](f: A => B) = wrap(f(value))
	override def mapValue[B](f: A => B) = map(f)
	
	override def appended[B >: A](elem: B) = wrapTwo(value, elem)
	override def prepended[B >: A](elem: B) = wrapTwo(elem, value)
}
