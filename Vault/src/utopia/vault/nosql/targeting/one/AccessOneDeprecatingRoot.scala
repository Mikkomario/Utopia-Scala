package utopia.vault.nosql.targeting.one

import utopia.vault.nosql.targeting.AccessDeprecatingRoot
import utopia.vault.nosql.view.DeprecatableView

/**
 * Common trait for root level access classes that target individual deprecated or active items at once
 *
 * @author Mikko Hilpinen
 * @since 19.08.2025, v2.0
 */
trait AccessOneDeprecatingRoot[+A <: DeprecatableView[A]] extends OneDeprecatingRoot[A] with AccessDeprecatingRoot[A]