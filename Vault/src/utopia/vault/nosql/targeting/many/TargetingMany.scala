package utopia.vault.nosql.targeting.many

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.Targeting
import utopia.vault.nosql.targeting.one.TargetingOne

object TargetingMany
{
	// OTHER    -------------------------
	
	def map[O, R](original: TargetingMany[O])(f: O => R): TargetingMany[R] = new Wrapper(original)(f)
	
	
	// NESTED   -------------------------
	
	private class Wrapper[O, +R](override val wrapped: TargetingMany[O])(f: O => R)
		extends TargetingMany[R]
			with TargetingManyWrapper[TargetingMany[O], TargetingOne[Option[O]], O, R, TargetingMany[R], TargetingOne[Option[R]]]
	{
		override protected def self: TargetingMany[R] = this
		override protected def mapResult(result: O): R = f(result)
		
		override protected def wrap(newTarget: TargetingMany[O]): TargetingMany[R] = new Wrapper(newTarget)(f)
		override protected def wrapUniqueTarget(target: TargetingOne[Option[O]]): TargetingOne[Option[R]] =
			target.mapResult { _.map(f) }
	}
}

/**
  * Common trait for extendable access points that yield multiple items at a time
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait TargetingMany[+A]
	extends Targeting[Seq[A], Seq[Value], Seq[Seq[Value]]]
		with TargetingManyLike[A, TargetingMany[A], TargetingOne[Option[A]]]
{
	/**
	  * @param f A mapping function applied to this access point's results
	  * @tparam B Type of mapping results
	  * @return An access point that yields mapped results
	  */
	def map[B](f: A => B): TargetingMany[B] = TargetingMany.map(this)(f)
}