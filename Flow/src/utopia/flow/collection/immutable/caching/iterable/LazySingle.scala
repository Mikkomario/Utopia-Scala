package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.immutable._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.{Lazy, LazyWrapper}

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable

object LazySingle
{
	/**
	 * @param getValue A function that yields the value to wrap
	 * @tparam A Type of the accessed value
	 * @return A lazy collection wrapping the specified value
	 */
	def apply[A](getValue: => A) = wrap(Lazy(getValue))
	
	/**
	 * @param view A view into the value to wrap
	 * @tparam A Type of the wrapped value
	 * @return A lazy collection caching the specified value or wrapping the specified view
	 */
	def from[A](view: View[A]) = view match {
		case ls: LazySingle[A] => ls
		case l: Lazy[A] => wrap(l)
		case v => apply(v.value)
	}
	
	/**
	 * @param lazyValue A lazy container
	 * @tparam A Type of the wrapped value
	 * @return A lazy collection wrapping the specified container
	 */
	def wrap[A](lazyValue: Lazy[A]) = new LazySingle[A](lazyValue)
}

/**
  * A lazily initialized collection which contains only a single item
  * @author Mikko Hilpinen
  * @since 16.01.2026, v2.8
  */
class LazySingle[+A](override protected val wrapped: Lazy[A])
	extends Seq[A] with View[A] with SingleOps[A, Seq, Seq[A], IsEmpty, LazySingle, LazyPair, LazySingle[A]]
		with LazyWrapper[A]
{
	// IMPLEMENTED  ----------------------------
	
	override protected def self = this
	
	override protected def _empty = Empty
	
	override def iterableFactory = OptimizedIndexedSeq
	
	override def view = new SingleView[A](value)
	override protected def reversed = this
	
	override def toString = s"Single($wrapped)"
	
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] =
		OptimizedIndexedSeq.newBuilder
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		OptimizedIndexedSeq.from(coll)
	
	override protected def wrap[B](newValue: => B): LazySingle[B] = LazySingle(newValue)
	override protected def wrapTwo[B](first: => B, second: => B): LazyPair[B] = LazyPair(first, second)
	
	override def map[B](f: A => B) = LazySingle.wrap(wrapped.lightMap(f))
	
	override def take(n: Int) = if (n <= 0) _empty else self
	override def takeRight(n: Int) = take(n)
	override def takeWhile(p: A => Boolean) = filter(p)
	override def drop(n: Int) = if (n >= 1) _empty else self
	override def dropRight(n: Int) = drop(n)
	override def dropWhile(p: A => Boolean) = filterNot(p)
	
	// TODO: Could convert this to lazy
	override def flatMap[B](f: A => IterableOnce[B]) = OptimizedIndexedSeq.from(f(value))
	
	override def appendedAll[B >: A](suffix: IterableOnce[B]) = suffix.knownSize match {
		case 0 => this
		case 1 => LazyPair.wrap(wrapped, Lazy { suffix.iterator.next() })
		case _ =>
			def lazily = {
				val iter = suffix.iterator
				if (iter.hasNext)
					LazySeq(Iterator.single(wrapped) ++ iter.map(Lazy.initialized))
				else
					this
			}
			suffix match {
				case _: scala.collection.View[B] => lazily
				case l: LazySeqLike[B, Seq] => wrapped +: l
				case i: Iterable[B] =>
					LazyVector(OptimizedIndexedSeq.concat(Single(wrapped), i.view.map(Lazy.initialized)))
				case _ => lazily
			}
	}
	
	override def padTo[B >: A](len: Int, elem: B) = len match {
		case 2 => LazyPair.wrap(wrapped, Lazy.initialized(elem))
		case x if x <= 1 => this
		case _ => Vector.from(iterator ++ Iterator.fill(len)(elem))
	}
}
