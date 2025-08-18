package utopia.vault.nosql.targeting.one

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.AccessDeprecatingRoot
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.DeprecatableView

object AccessOneDeprecatingRoot
{
	// OTHER    ------------------------
	
	/**
	 * @param all Access to all items, including historical entries
	 * @tparam A Type of the wrapped access point
	 * @return A root level access, wrapping the specified access
	 */
	def apply[A <: DeprecatableView[A] with Indexed](all: A): AccessOneDeprecatingRoot[A] =
		_AccessOneDeprecatingRoot(all)
	
	
	// NESTED   ------------------------
	
	private case class _AccessOneDeprecatingRoot[+A <: DeprecatableView[A] with Indexed](all: A)
		extends AccessOneDeprecatingRoot[A]
}

/**
 * Common trait for root level access classes that target individual deprecated or active items at once
 * @author Mikko Hilpinen
 * @since 08.08.2025, v2.0
 */
trait AccessOneDeprecatingRoot[+A <: DeprecatableView[A] with Indexed] extends AccessDeprecatingRoot[A]
{
	// OTHER    -------------------------
	
	/**
	 * @param id ID of the targeted item
	 * @return Access to that item
	 */
	def apply(id: Int) = {
		val acc = all
		acc(acc.index <=> id)
	}
}
