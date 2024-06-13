package utopia.flow.collection.immutable

import utopia.flow.collection.CollectionExtensions._

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{View, mutable}

/**
  * A collection which contains only a single item
  * @author Mikko Hilpinen
  * @since 05.06.2024, v2.4
  */
case class Single[+A](value: A)
	extends IndexedSeq[A] with utopia.flow.view.immutable.View[A]
		with SingleOps[A, IndexedSeq, IndexedSeq[A], IsEmpty, Single, Pair, Single[A]]
{
	// IMPLEMENTED  ----------------------------
	
	override protected def self: Single[A] = this
	
	override protected def _empty = Empty
	
	override def iterableFactory = OptimizedIndexedSeq
	
	override def view = new SingleView[A](value)
	override protected def reversed = this
	
	override def toString() = s"Single($value)"
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] =
		OptimizedIndexedSeq.newBuilder
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		OptimizedIndexedSeq.from(coll)
	
	override protected def wrap[B](newValue: => B): Single[B] = Single(newValue)
	override protected def wrapTwo[B](first: => B, second: => B): Pair[B] = Pair(first, second)
	
	override def take(n: Int) = if (n <= 0) _empty else self
	override def takeRight(n: Int) = take(n)
	override def takeWhile(p: A => Boolean) = filter(p)
	override def drop(n: Int) = if (n >= 1) _empty else self
	override def dropRight(n: Int) = drop(n)
	override def dropWhile(p: A => Boolean) = filterNot(p)
	
	override def flatMap[B](f: A => IterableOnce[B]) = OptimizedIndexedSeq.from(f(value))
	
	override def appendedAll[B >: A](suffix: IterableOnce[B]) =
		_addAll(suffix)(Pair.apply) { _.iterator ++ _ }
	override def prependedAll[B >: A](prefix: IterableOnce[B]) =
		_addAll(prefix) { (a, b) => Pair(b, a) } { (a, b) => b.iterator ++ a }
	
	override def padTo[B >: A](len: Int, elem: B) = len match {
		case 2 => Pair(value, elem)
		case x if x <= 1 => this
		case _ => Vector.from(iterator ++ Iterator.fill(len)(elem))
	}
	
	
	// OTHER    ----------------------------
	
	private def _addAll[B >: A](c: IterableOnce[B])(toPair: (A, B) => Pair[B])
	                           (combine: (IterableOnce[A], IterableOnce[B]) => IterableOnce[B]): IndexedSeq[B] =
		c.knownSize match {
			case 0 => this
			case 1 => toPair(value, c.iterator.next())
			case x if x > 1 => Vector.from(combine(this, c))
			case _ =>
				c match {
					case v: View[B] =>
						val iter = v.iterator
						if (iter.hasNext)
							OptimizedIndexedSeq.from(combine(this, iter))
						else
							this
					case i: Iterable[B] =>
						i.emptyOneOrMany match {
							case None => this
							case Some(Left(only)) => toPair(value, only)
							case Some(Right(many)) => Vector.from(combine(this, many))
						}
					case i =>
						val iter = i.iterator
						if (iter.hasNext)
							OptimizedIndexedSeq.from(combine(this, iter))
						else
							this
				}
		}
}
