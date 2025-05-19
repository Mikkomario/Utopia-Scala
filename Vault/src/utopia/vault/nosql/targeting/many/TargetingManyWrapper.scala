package utopia.vault.nosql.targeting.many

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.TargetingWrapper

/**
  * Common trait for access points that target multiple items at a time by wrapping another targeted access point
  * @author Mikko Hilpinen
  * @since 15.05.2025, v1.21
  */
trait TargetingManyWrapper[T <: TargetingManyLike[O, T], O, +A, +Repr]
	extends TargetingManyLike[A, Repr] with TargetingWrapper[T, Seq[O], Seq[Value], Seq[A], Seq[Value], Repr]
{
	// ABSTRACT ---------------------------
	
	protected def mapResult(result: O): A
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def wrapResult(result: Seq[O]): Seq[A] = result.map(mapResult)
	override protected def wrapValue(value: Seq[Value]): Seq[Value] = value
}
