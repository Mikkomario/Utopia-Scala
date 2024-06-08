package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.immutable.IndexedSeqOps
import scala.collection.{View, mutable}

/**
  * A collection which contains only a single item
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
case class Single[+A](value: A)
	extends IndexedSeqOps[A, IndexedSeq, IndexedSeq[A]] with IndexedSeq[A] with utopia.flow.view.immutable.View[A]
{
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
	
	override def iterableFactory = OptimizedIndexedSeq
	
	override def iterator = Iterator.single(value)
	override def valueIterator = iterator
	override def reverseIterator = iterator
	override def tail = Empty
	
	override def empty = Empty
	override def reverse = this
	override protected def reversed = this
	override def distinct = this
	
	override def zipWithIndex = Single(value -> 0)
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] =
		OptimizedIndexedSeq.newBuilder
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		OptimizedIndexedSeq.from(coll)
	
	override def isDefinedAt(idx: Int) = idx == 0
	override def apply(v1: Int) = value
	
	override def distinctBy[B](f: A => B) = this
	override def sorted[B >: A](implicit ord: Ordering[B]) = this
	override def sortWith(lt: (A, A) => Boolean) = this
	override def sortBy[B](f: A => B)(implicit ord: Ordering[B]) = this
	
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
	
	override def take(n: Int) = if (n <= 0) Empty else this
	override def takeRight(n: Int) = take(n)
	override def takeWhile(p: A => Boolean) = filter(p)
	override def drop(n: Int) = if (n >= 1) Empty else this
	override def dropRight(n: Int) = drop(n)
	override def dropWhile(p: A => Boolean) = filterNot(p)
	
	override def filter(pred: A => Boolean) = if (pred(value)) this else Empty
	override def filterNot(pred: A => Boolean) = if (pred(value)) Empty else this
	
	override def zip[B](that: IterableOnce[B]) = that.iterator.nextOption() match {
		case Some(other) => Single(value -> other)
		case None => Empty
	}
	
	override def map[B](f: A => B) = Single(f(value))
	override def mapValue[B](f: A => B) = map(f)
	override def flatMap[B](f: A => IterableOnce[B]) = OptimizedIndexedSeq.from(f(value))
	
	override def appended[B >: A](elem: B) = Pair(value, elem)
	override def prepended[B >: A](elem: B) = Pair(elem, value)
	
	override def appendedAll[B >: A](suffix: IterableOnce[B]) = suffix match {
		case i: Iterable[B] =>
			i.emptyOneOrMany match {
				case None => this
				case Some(Left(only)) => Pair(value, only)
				case Some(Right(many)) => Vector.from(View.concat(this, many))
			}
		case i =>
			val iter = i.iterator
			if (iter.hasNext)
				OptimizedIndexedSeq.from(iterator ++ iter)
			else
				this
	}
	// WET WET
	override def prependedAll[B >: A](prefix: IterableOnce[B]) = prefix match {
		case i: Iterable[B] =>
			i.emptyOneOrMany match {
				case None => this
				case Some(Left(only)) => Pair(only, value)
				case Some(Right(many)) => Vector.from(View.concat(many, this))
			}
		case i =>
			val iter = i.iterator
			if (iter.hasNext)
				OptimizedIndexedSeq.from(iter ++ iterator)
			else
				this
	}
	
	override def padTo[B >: A](len: Int, elem: B) = len match {
		case 2 => Pair(value, elem)
		case x if x <= 1 => this
		case _ => Vector.from(iterator ++ Iterator.fill(len)(elem))
	}
}
