package utopia.vault.nosql.targeting

import utopia.vault.model.template.Deprecates
import utopia.vault.nosql.view.DeprecatableView

/**
 * A simplified version of [[DeprecatingRoot]] for views which specify deprecation conditions themselves
 * @author Mikko Hilpinen
 * @since 19.08.2025, v2.0
 */
trait AccessDeprecatingRoot[+A <: DeprecatableView[A]] extends DeprecatingRoot[A]
{
	override def model: Deprecates = all.model
}
