package utopia.vault.nosql.targeting.many

import utopia.vault.nosql.targeting.AccessDeprecatingRoot
import utopia.vault.nosql.view.DeprecatableView

/**
 * Common trait for root level access points which target historical or active rows, or both.
 *
 * @author Mikko Hilpinen
 * @since 19.08.2025, v2.0
 */
trait AccessManyDeprecatingRoot[+A <: DeprecatableView[A]]
	extends ManyDeprecatingRoot[A] with AccessDeprecatingRoot[A]
