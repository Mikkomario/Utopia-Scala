package utopia.flow.operator.filter

import utopia.flow.collection.CollectionExtensions.{iterableOperations2, _}
import utopia.flow.util.NotEmpty

/**
 * This filter will accept any item (i.e. returns true for all items)
 * @author Mikko Hilpinen
 * @since Inception 21.1.2017, added to Flow 30.1.2024, v2.4
 */
object AcceptAll extends Filter[Any]
{
	override def apply(item: Any) = true
	
	override def unary_! = RejectAll
	
	override def or[B <: Any](other: Filter[B]) = this
	override def or[B <: Any](other: Filter[B], more: Filter[B]*) = this
	override def or[B <: Any](filters: IterableOnce[Filter[B]]) = this
	
	override def and[B <: Any](other: Filter[B]) = other
	override def and[B <: Any](other: Filter[B], more: Filter[B]*) = NotEmpty(more) match {
		case Some(more) => other and more
		case None => other
	}
	override def and[B <: Any](filters: IterableOnce[Filter[B]]) = Seq.from(filters).emptyOneOrMany match {
		case None => this
		case Some(Left(only)) => only
		case Some(Right(many)) => many.head and many.tail
	}
}