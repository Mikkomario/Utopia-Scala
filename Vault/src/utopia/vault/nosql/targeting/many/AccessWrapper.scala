package utopia.vault.nosql.targeting.many

import utopia.vault.nosql.targeting.one.TargetingOne

/**
  * Common trait for interfaces that wrap a many-access point, providing a more advanced interface it,
  * as well as for the unique access version.
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait AccessWrapper[A, +Repr <: TargetingMany[A], +One <: TargetingOne[Option[A]]]
	extends TargetingMany[A] with TargetingManyWrapper[TargetingMany[A], TargetingOne[Option[A]], A, A, Repr, One]
{
	override protected def mapResult(result: A): A = result
}
