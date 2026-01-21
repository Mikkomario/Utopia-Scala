package utopia.vault.nosql.targeting.grouped

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.Targeting

/**
 * Common trait for targeting access points which retrieve the contents of multiple rows at once
 * @tparam A Type of pulled items / groups
 * @author Mikko Hilpinen
 * @since 21.01.2026, v2.1
 */
trait TargetingGrouped[+A]
	extends Targeting[A, Seq[Value], Seq[Seq[Value]]] with TargetingGroupedLike[A, TargetingGrouped[A]]
