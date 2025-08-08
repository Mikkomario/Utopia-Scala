package utopia.vault.nosql.targeting.many

import utopia.vault.model.immutable.Table
import utopia.vault.model.template.HasTable
import utopia.vault.nosql.targeting.AccessDeprecatingRoot
import utopia.vault.nosql.view.{DeprecatableView, ViewManyByIntIds}
import utopia.vault.sql.Condition

/**
 * Common trait for root level access points which target historical or active rows, or both.
 *
 * @author Mikko Hilpinen
 * @since 08.08.2025, v2.0
 */
trait AccessManyDeprecatingRoot[+A <: DeprecatableView[A] with HasTable]
	extends AccessDeprecatingRoot[A] with ViewManyByIntIds[A]
{
	// IMPLEMENTED  ---------------------
	
	override def table: Table = all.table
	
	override def apply(condition: Condition): A = all(condition)
}
