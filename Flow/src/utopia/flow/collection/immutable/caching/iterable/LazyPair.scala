package utopia.flow.collection.immutable.caching.iterable

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{OptimizedIndexedSeq, Pair, PairOps, PairView}
import utopia.flow.collection.mutable.builder.LazyBuilder
import utopia.flow.collection.template.factory.LazyFactory
import utopia.flow.operator.enumeration.End
import utopia.flow.view.immutable.caching.Lazy

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{SeqFactory, mutable}

object LazyPair
{
	// COMPUTED -----------------------------
	
	/**
	  * @return Factory for creating indexed sequences, possibly pairs
	  */
	def factory = LazyPairFactory
	/**
	  * @tparam A Type of items added
	  * @return A builder for creating pairs or other indexed sequences
	  */
	def newBuilder[A] = new LazyBuilder[A, IndexedSeq[A]](items => factory(items))
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param first First item to wrap (call-by-name / lazy)
	  * @param second Second item to wrap (call-by-name / lazy)
	  * @tparam A Type of items held within this pair
	  * @return A new pair that contains those two values, lazily initialized
	  */
	def apply[A](first: => A, second: => A) = wrap[A](Lazy(first), Lazy(second))
	/**
	  * @param first First lazy container
	  * @param second Second lazy container
	  * @tparam A Type of items held within this pair
	  * @return A new pair containing those two lazy containers
	  */
	def wrap[A](first: Lazy[A], second: Lazy[A]) = new LazyPair[A](first, second)
	
	/**
	  * @param value Value to hold on both sides of this pair (call-by-name / lazy)
	  * @tparam A Type of the specified item
	  * @return A pair containing the specified value on both sides
	  */
	def twice[A](value: => A) = wrapTwice(Lazy(value))
	/**
	  * @param lazyValue Lazy container to place on both sides of this pair
	  * @tparam A Type of the specified item
	  * @return A pair containing the specified lazy container on both sides
	  */
	def wrapTwice[A](lazyValue: Lazy[A]) = new LazyPair[A](lazyValue, lazyValue)
	
	/**
	  * @param value Function for specifying the values on this pair (call-by-name, called twice, lazily)
	  * @tparam A Type of the values held in this pair
	  * @return A pair with a value generated with the specified function on both sides
	  */
	def fill[A](value: => A) = new LazyPair[A](Lazy(value), Lazy(value))
	
	
	// NESTED   -----------------------------
	
	object LazyPairFactory extends LazyFactory[IndexedSeq] with SeqFactory[IndexedSeq]
	{
		// IMPLEMENTED  ---------------------
		
		override def empty[A]: IndexedSeq[A] = LazyVector.empty
		override def apply[A](): IndexedSeq[A] = empty
		
		override def apply[A](items: IterableOnce[Lazy[A]]): IndexedSeq[A] = items match {
			case p: Pair[Lazy[A]] => new LazyPair[A](p)
			case _ =>
				if (items.knownSize == 2) {
					val iter = items.iterator
					new LazyPair[A](Pair(iter.next(), iter.next()))
				}
				else
					LazyVector(items)
		}
		
		override def from[A](source: IterableOnce[A]): IndexedSeq[A] = source match {
			case s: LazySeqLike[A, _] with IndexedSeq[A] => s
			case p: Pair[A] => new LazyPair[A](p.map(Lazy.initialized))
			case s: Seq[A] =>
				if (s hasSize 2)
					new LazyPair[A](Lazy(s.head), Lazy(s(1)))
				else
					LazyVector.from(s)
			case o =>
				if (o.knownSize == 2) {
					val iter = o.iterator
					new LazyPair[A](Lazy.initialized(iter.next()), Lazy(iter.next()))
				}
				else
					LazyVector.from(o)
		}
		
		override def newBuilder[A]: mutable.Builder[A, IndexedSeq[A]] = OptimizedIndexedSeq.newBuilder[A].mapResult {
			case p: Pair[A] => new LazyPair[A](p.map(Lazy.initialized))
			case seq => LazyVector.from(seq)
		}
	}
}

/**
  * Contains 2 lazily initialized values
  * @author Mikko Hilpinen
  * @since 02.06.2024, v2.4
  */
class LazyPair[+A](override val lazyContents: Pair[Lazy[A]])
	extends LazySeqLike[A, IndexedSeq] with PairOps[A, IndexedSeq, IndexedSeq[A], LazyPair, LazyPair[A]]
		with IndexedSeq[A]
{
	// COMPUTED -------------------------------
	
	/**
	  * @return Lazy container that holds the first item in this pair
	  */
	def lazyFirst = lazyContents.first
	/**
	  * @return Lazy container that holds the second item in this pair
	  */
	def lazySecond = lazyContents.second
	
	/**
	  * @return The items which have already been initialized within this pair
	  */
	def current = lazyContents.flatMap { _.current }
	
	
	// IMPLEMENTED  ---------------------------
	
	override def self: LazyPair[A] = this
	
	override def first: A = lazyFirst.value
	override def second: A = lazySecond.value
	
	override protected def factory = LazyPair.factory
	override protected def _empty: IndexedSeq[A] = factory.empty
	
	override def unary_- : LazyPair[A] = new LazyPair[A](lazyContents.reverse)
	
	override protected def only(side: End): IndexedSeq[A] = LazyVector(lazyContents(side))
	override protected def newPair[B](first: => B, second: => B): LazyPair[B] =
		new LazyPair[B](Pair(Lazy(first), Lazy(second)))
	
	override protected def _fromSpecific(coll: IterableOnce[A @uncheckedVariance]): IndexedSeq[A] =
		LazyPair.factory.from(coll)
	override protected def _newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] =
		LazyPair.factory.newBuilder[A]
	override protected def fromSpecific(coll: IterableOnce[A @uncheckedVariance]) = _fromSpecific(coll)
	override protected def newSpecificBuilder: mutable.Builder[A @uncheckedVariance, IndexedSeq[A]] = _newSpecificBuilder
	
	override def :+[B >: A](item: Lazy[B]) = LazyVector(lazyContents :+ item)
	override def +:[B >: A](item: Lazy[B]) = LazyVector(item +: lazyContents)
	
	override def lazyAppendAll[B >: A](items: IterableOnce[Lazy[B]]) =
		items.nonEmptyIterator match {
			case Some(iter) => LazyVector(lazyContents ++ iter)
			case None => self
		}
	override def lazyPrependAll[B >: A](items: IterableOnce[Lazy[B]]) =
		items match {
			case i: Iterable[Lazy[B]] => if (i.isEmpty) self else LazyVector(i ++ lazyContents)
			case i =>
				i.nonEmptyIterator match {
					case Some(iter) => LazyVector(iter ++ lazyContents)
					case None => self
				}
		}
	
	override def withFirst[B >: A](newFirst: B) = new LazyPair[B](Lazy.initialized(newFirst), lazySecond)
	override def withSecond[B >: A](newSecond: B) = new LazyPair[B](lazyFirst, Lazy.initialized(newSecond))
	
	override def mapFirst[B >: A](f: A => B) = new LazyPair[B](lazyFirst.map(f), lazySecond)
	override def mapSecond[B >: A](f: A => B) = new LazyPair[B](lazyFirst, lazySecond.map(f))
	
	override def view = new PairView[A](first, second)
	
	override def prepended[B >: A](elem: B) = LazyVector(Lazy.initialized(elem) +: lazyContents)
	override def appended[B >: A](elem: B) = LazyVector(lazyContents :+ Lazy.initialized(elem))
	
	override def prependedAll[B >: A](prefix: IterableOnce[B]) =
		prefix match {
			case i: Iterable[B] => if (i.isEmpty) self else LazyVector.from(i ++ this)
			case i =>
				i.nonEmptyIterator match {
					case Some(iter) => LazyVector.from(iter ++ this)
					case None => self
				}
		}
	override def appendedAll[B >: A](suffix: IterableOnce[B]) =
		suffix.nonEmptyIterator match {
			case Some(iter) => LazyVector.from(iterator ++ iter)
			case None => self
		}
	
	override def filter(pred: A => Boolean) = {
		if (pred(first)) {
			if (pred(second))
				this
			else
				LazyVector(lazyFirst)
		}
		else if (pred(second))
			LazyVector(lazySecond)
		else
			LazyVector.empty
	}
}
