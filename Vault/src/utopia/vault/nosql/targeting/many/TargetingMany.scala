package utopia.vault.nosql.targeting.many

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.Targeting

/**
  * Common trait for extendable access points that yield multiple items at a time
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait TargetingMany[+A] extends Targeting[Seq[A], Seq[Value]] with TargetingManyLike[A, TargetingMany[A]]
