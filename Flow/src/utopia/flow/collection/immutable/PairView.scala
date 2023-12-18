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
	// IMPLEMENTED  ------------------------
	
	override def first: A = firstView
	override def second: A = secondView
	
	override def self: PairView[A] = this
	override def unary_- : PairView[A] = new PairView[A](second, first)
	
	override protected def _empty: IndexedSeqView[A] = new EmptyIndexedSeqView[A]
	
	override protected def only(side: End): IndexedSeqView[A] = new SideView(side)
	override protected def newPair[B](first: => B, second: => B): PairView[B] = new PairView[B](first, second)
	
	override protected def _fromSpecific(coll: IterableOnce[A @uncheckedVariance]) =
		super[AbstractIndexedSeqView].fromSpecific(coll)
	override protected def _newSpecificBuilder: mutable.Builder[A @uncheckedVariance, collection.View[A]] =
		super[AbstractIndexedSeqView].newSpecificBuilder
	
	
	// NESTED   ------------------------------
	
	private class SideView(side: End) extends IndexedSeqView[A]
	{
		private def value = PairView.this(side)
		
		override def length: Int = 1
		override def apply(i: Int): A =
			if (i == 0) value else throw new IndexOutOfBoundsException(s"Index $i is out of bounds (length = 1)")
	}
}
