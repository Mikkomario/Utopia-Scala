package utopia.vault.nosql.targeting.many

import utopia.vault.model.immutable.Table
import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.targeting.DeprecatingRoot
import utopia.vault.nosql.targeting.DeprecatingRoot.DeprecatingRootFactory
import utopia.vault.nosql.view.{FilterableView, ViewManyByIntIds}
import utopia.vault.sql.Condition

object AccessManyDeprecatingRoot extends DeprecatingRootFactory[AccessManyDeprecatingRoot]
{
	// IMPLEMENTED    --------------------
	
	override protected def _apply[A <: FilterableView[A]](all: A, conditions: Deprecates): AccessManyDeprecatingRoot[A] =
		_AccessManyDeprecatingRoot(all, conditions)
	
	
	// NESTED   -------------------------
	
	private case class _AccessManyDeprecatingRoot[+A <: FilterableView[A]](all: A, model: Deprecates)
		extends AccessManyDeprecatingRoot[A]
}

/**
 * Common trait for root level access points which target historical or active rows, or both.
 *
 * @author Mikko Hilpinen
 * @since 08.08.2025, v2.0
 */
trait AccessManyDeprecatingRoot[+A <: FilterableView[A]]
	extends DeprecatingRoot[A] with ViewManyByIntIds[A]
{
	// IMPLEMENTED  ---------------------
	
	override def table: Table = all.table
	
	override def apply(condition: Condition): A = all(condition)
}
