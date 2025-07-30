package utopia.vault.nosql.targeting.many

import utopia.flow.generic.model.immutable.Value
import utopia.flow.operator.enumeration.{End, Extreme}
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.targeting.TargetingWrapper
import utopia.vault.sql.{Condition, OrderBy}

/**
  * Common trait for access points that target multiple items at a time by wrapping another targeted access point
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingManyWrapper[T <: TargetingManyLike[O, T, OT], OT, O, +A, +Repr, +One]
	extends TargetingManyLike[A, Repr, One] with TargetingWrapper[T, Seq[O], Seq[Value], Seq[A], Seq[Value], Repr]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @param result An item accessed by the wrapped target
	  * @return A mapped item
	  */
	protected def mapResult(result: O): A
	/**
	  * @param target A target for accessing individual items
	  * @return A wrapped individual access
	  */
	protected def wrapUniqueTarget(target: OT): One
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def wrapResult(result: Seq[O]): Seq[A] = result.map(mapResult)
	override protected def wrapValue(value: Seq[Value]): Seq[Value] = value
	
	override def apply(end: End, ordering: Option[OrderBy], filter: Option[Condition]) =
		wrapUniqueTarget(wrapped(end, ordering, filter))
	
	override def apply(column: Column, extreme: Extreme)(implicit connection: Connection): Value =
		wrapped(column, extreme)
	
	override def streamColumn[B](column: Column, distinct: Boolean)(f: Iterator[Value] => B)
	                            (implicit connection: Connection) =
		wrapped.streamColumn(column, distinct)(f)
	override def streamColumns[B](columns: Seq[Column])(f: Iterator[Seq[Value]] => B)
	                             (implicit connection: Connection) =
		wrapped.streamColumns(columns)(f)
}
