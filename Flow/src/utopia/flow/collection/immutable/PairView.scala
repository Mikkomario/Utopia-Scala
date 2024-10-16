package utopia.flow.collection.immutable

import utopia.flow.operator.enumeration.End

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.{AbstractIndexedSeqView, IndexedSeqView, mutable}

/**
  * A view into a pair
  * @author Mikko Hilpinen
  * @since 18.12.2023, v2.3
  */
class PairView[+A](firstView: => A, secondView: => A)
	extends AbstractIndexedSeqView[A] with PairOps[A, collection.View, collection.View[A], PairView, PairView[A]]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return A pair containing the viewed values
	  */
	def toPair = Pair(firstView, secondView)
	
	
	// IMPLEMENTED  ------------------------
	
	override def first: A = firstView
	override def second: A = secondView
	
	override def self: PairView[A] = this
	override def unary_- : PairView[A] = new PairView[A](second, first)
	
	override protected def _empty = EmptyView
	
	override def toSeq = toPair
	override def toIndexedSeq = toPair
	
	override protected def only(side: End): IndexedSeqView[A] = new SingleView[A](apply(side))
	override protected def newPair[B](first: => B, second: => B): PairView[B] = new PairView[B](first, second)
	
	override protected def _fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		super[AbstractIndexedSeqView].fromSpecific(coll)
	override protected def _newSpecificBuilder: mutable.Builder[A @uncheckedVariance, collection.View[A]] =
		super[AbstractIndexedSeqView].newSpecificBuilder
	
	override def drop(n: Int) = super[AbstractIndexedSeqView].drop(n)
	override def dropRight(n: Int) = super[AbstractIndexedSeqView].dropRight(n)
	
	override def take(n: Int) = super[AbstractIndexedSeqView].take(n)
	override def takeRight(n: Int) = super[AbstractIndexedSeqView].takeRight(n)
}
