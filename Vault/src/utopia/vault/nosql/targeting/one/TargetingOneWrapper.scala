package utopia.vault.nosql.targeting.one

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.TargetingWrapper

/**
  * Common trait for instances that implement [[TargetingOneLike]] by wrapping one.
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait TargetingOneWrapper[T <: TargetingOneLike[O, T], O, +R, +Repr]
	extends TargetingOneLike[R, Repr] with TargetingWrapper[T, O, Value, R, Value, Repr]
{
	override protected def wrapValue(value: Value): Value = value
}
