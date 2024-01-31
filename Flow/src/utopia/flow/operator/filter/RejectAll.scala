package utopia.flow.operator.filter

import utopia.flow.util.NotEmpty

/**
  * This filter rejects all items. I.e. returns false for all items.
  * @author Mikko Hilpinen
  * @since 30/01/2024, v2.4
  */
object RejectAll extends Filter[Any]
{
	override def unary_! = AcceptAll
	
	override def apply(item: Any): Boolean = false
	
	override def or[B <: Any](other: Filter[B]) = other
	override def or[B <: Any](other: Filter[B], more: Filter[B]*) = NotEmpty(more) match {
		case Some(more) => other or more
		case None => other
	}
	override def or[B <: Any](filters: IterableOnce[Filter[B]]) = NotEmpty(Seq.from(filters)) match {
		case Some(filters) => filters.head or filters.tail
		case None => this
	}
	
	override def and[B <: Any](other: Filter[B]) = this
	override def and[B <: Any](other: Filter[B], more: Filter[B]*) = this
	override def and[B <: Any](filters: IterableOnce[Filter[B]]) = this
}
