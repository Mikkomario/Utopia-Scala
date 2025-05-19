package utopia.vault.nosql.targeting.one

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.Targeting

// TODO: Possibly, add implicitly map function for instances which yield an option
object TargetingOne
{
	// OTHER    ----------------------------
	
	def map[O, R](original: TargetingOne[O])(f: O => R): TargetingOne[R] = new Wrapper[O, R](original, f)
	
	
	// NESTED   ----------------------------
	
	private class Wrapper[O, +R](override val wrapped: TargetingOne[O], f: O => R)
		extends TargetingOne[R] with TargetingOneWrapper[TargetingOne[O], O, R, TargetingOne[R]]
	{
		override protected def self: TargetingOne[R] = this
		
		override protected def wrapResult(result: O): R = f(result)
		override protected def wrap(newTarget: TargetingOne[O]): TargetingOne[R] = new Wrapper(newTarget, f)
	}
}

/**
  * Common trait for access point that yield individual items with each query
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait TargetingOne[+A] extends Targeting[A, Value] with TargetingOneLike[A, TargetingOne[A]]
{
	override def mapResult[B](f: A => B) = TargetingOne.map(this)(f)
}
