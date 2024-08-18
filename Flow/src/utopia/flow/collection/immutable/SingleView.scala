package utopia.flow.collection.immutable

import utopia.flow.view.immutable.View

import scala.collection.{AbstractIndexedSeqView, IndexedSeqView}

/**
  * Represents a view into a singular item (a Single)
  * @author Mikko Hilpinen
  * @since 13.06.2024, v2.4
  */
class SingleView[+A](accessValue: => A)
	extends AbstractIndexedSeqView[A] with View[A]
		with SingleOps[A, scala.collection.View, scala.collection.View[A], IsEmptyView, SingleView, PairView, SingleView[A]]
{
	// COMPUTED ----------------------------
	
	/**
	  * @return Viewed item wrapped in a Single container
	  */
	def toSingle = Single(accessValue)
	
	
	// IMPLEMENTED  ------------------------
	
	override protected def self = this
	
	override def value: A = accessValue
	
	override protected def _empty = EmptyView
	
	override def toSeq = toSingle
	override def toIndexedSeq = toSingle
	
	override protected def wrap[B](newValue: => B): SingleView[B] = new SingleView[B](newValue)
	override protected def wrapTwo[B](first: => B, second: => B): PairView[B] = new PairView[B](first, second)
	
	override def slice(from: Int, until: Int): IndexedSeqView[A] = if (from > 0 || until <= 0) _empty else this
}
