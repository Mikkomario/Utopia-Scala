package utopia.vault.nosql.targeting.many

import utopia.vault.nosql.targeting.one.TargetingOne

object TargetingManyRows
{
	// OTHER    ----------------------
	
	def map[O, R](target: TargetingManyRows[O])(f: O => R): TargetingManyRows[R] = new Wrapper[O, R](target)(f)
	
	
	// NESTED   ----------------------
	
	private class Wrapper[O, +R](override val wrapped: TargetingManyRows[O])(f: O => R)
		extends TargetingManyRows[R]
			with TargetingManyRowsWrapper[TargetingManyRows[O], TargetingOne[Option[O]], O, R, TargetingManyRows[R], TargetingOne[Option[R]]]
	{
		override protected def self: TargetingManyRows[R] = this
		
		override protected def mapResult(result: O): R = f(result)
		override protected def wrap(newTarget: TargetingManyRows[O]): TargetingManyRows[R] = new Wrapper(newTarget)(f)
		override protected def wrapUniqueTarget(target: TargetingOne[Option[O]]): TargetingOne[Option[R]] =
			target.mapResult { _.map(f) }
	}
}

/**
  * Common trait for access points that yield multiple row-specific items at a time
  * @author Mikko Hilpinen
  * @since 18.05.2025, v1.21
  */
trait TargetingManyRows[+A]
	extends TargetingMany[A] with TargetingManyRowsLike[A, TargetingManyRows[A], TargetingOne[Option[A]]]
{
	override def map[B](f: A => B): TargetingManyRows[B] = TargetingManyRows.map(this)(f)
}