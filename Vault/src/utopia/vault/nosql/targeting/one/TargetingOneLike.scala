package utopia.vault.nosql.targeting.one

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.TargetingLike

/**
  * Common trait for access points that yield individual items with each query
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait TargetingOneLike[+A, +Repr] extends TargetingLike[A, Value, Repr]
