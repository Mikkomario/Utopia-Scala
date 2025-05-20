package utopia.vault.nosql.targeting.many

import utopia.vault.nosql.targeting.one.TargetingOne

/**
  * Common trait for interfaces that wrap a row-based many-access point, providing a more advanced interface for it,
  * as well as for the single row access version.
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait AccessRowsWrapper[A, +Repr <: TargetingManyRows[A], +One <: TargetingOne[Option[A]]]
	extends TargetingManyRows[A] with TargetingManyRowsLike[A, Repr, One]
		with TargetingManyRowsWrapper[TargetingManyRows[A], TargetingOne[Option[A]], A, A, Repr, One]
