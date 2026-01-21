package utopia.vault.nosql.targeting.many

import utopia.flow.operator.enumeration.End
import utopia.vault.nosql.targeting.grouped.TargetingGroupedWrapper
import utopia.vault.sql.{Condition, OrderBy}

/**
  * Common trait for access points that target multiple items at a time by wrapping another targeted access point
  * @tparam T The type of the wrapped access point (accessing many items of O)
 * @tparam OT Type of the wrapped access point's single-access version (accessing one O)
 * @tparam O Type of the individual accessed items via the wrapped access points (T and OT)
 * @tparam A Type of individual wrapped / mapped items (O => A)
 * @tparam Repr Concrete / implementing access point type yielded by various copy functions
 * @tparam One Type of the access point that yields individual items (of type A);
 *             The result type of various single-access functions like .head.
 * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingManyWrapper[T <: TargetingManyLike[O, T, OT], OT, O, +A, +Repr, +One]
	extends TargetingGroupedWrapper[T, Seq[O], Seq[A], Repr] with TargetingManyLike[A, Repr, One]
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
	
	override def apply(end: End, ordering: Option[OrderBy], filter: Option[Condition]) =
		wrapUniqueTarget(wrapped(end, ordering, filter))
}
