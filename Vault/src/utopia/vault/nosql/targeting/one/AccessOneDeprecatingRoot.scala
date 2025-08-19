package utopia.vault.nosql.targeting.one

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.model.error.ColumnNotFoundException
import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.targeting.DeprecatingRoot
import utopia.vault.nosql.targeting.DeprecatingRoot.DeprecatingRootFactory
import utopia.vault.nosql.view.FilterableView

object AccessOneDeprecatingRoot extends DeprecatingRootFactory[AccessOneDeprecatingRoot]
{
	// IMPLEMENTED    ------------------
	
	override protected def _apply[A <: FilterableView[A]](all: A, conditions: Deprecates): AccessOneDeprecatingRoot[A] =
		_AccessOneDeprecatingRoot(all, conditions)
	
	
	// NESTED   ------------------------
	
	private case class _AccessOneDeprecatingRoot[+A <: FilterableView[A]](all: A, model: Deprecates)
		extends AccessOneDeprecatingRoot[A]
}

/**
 * Common trait for root level access classes that target individual deprecated or active items at once
 * @author Mikko Hilpinen
 * @since 08.08.2025, v2.0
 */
trait AccessOneDeprecatingRoot[+A <: FilterableView[A]] extends DeprecatingRoot[A]
{
	// OTHER    -------------------------
	
	/**
	 * @param id ID of the targeted item
	 * @return Access to that item
	 */
	def apply(id: Int) = {
		val acc = all
		acc.table.primaryColumn match {
			case Some(index) => acc(index <=> id)
			case None => throw new ColumnNotFoundException(s"Table ${ acc.table } doesn't specify a primary index")
		}
	}
}
