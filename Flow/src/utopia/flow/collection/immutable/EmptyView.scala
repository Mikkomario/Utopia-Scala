package utopia.flow.collection.immutable

import scala.collection.AbstractIndexedSeqView

/**
  * An empty view
  */
object EmptyView extends IsEmptyView

/**
  * Common trait for empty views
  * @author Mikko Hilpinen
  * @since 13.06.2024, v2.4
  */
trait IsEmptyView
	extends AbstractIndexedSeqView[Nothing]
		with EmptyOps[scala.collection.View, scala.collection.View[Nothing], SingleView, IsEmptyView]
{
	override protected def self = this
	
	override def toSeq = Empty
	override def toIndexedSeq = Empty
	
	override protected def wrapSingle[B](value: => B): SingleView[B] = new SingleView[B](value)
}
