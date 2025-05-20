package utopia.vault.nosql.targeting.one

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.many.TargetingManyLike
import utopia.vault.nosql.targeting.{Targeting, TargetingWrapper}

// TODO: Possibly, add implicitly map function for instances which yield an option
object TargetingOne
{
	// OTHER    ----------------------------
	
	def headOf[T <: TargetingManyLike[A, T, _], A](target: T): TargetingOne[Option[A]] = new Head[T, A](target)
	
	def map[O, R](original: TargetingOne[O])(f: O => R): TargetingOne[R] = new Wrapper[O, R](original, f)
	
	
	// NESTED   ----------------------------
	
	private class Wrapper[O, +R](override val wrapped: TargetingOne[O], f: O => R)
		extends TargetingOne[R] with TargetingOneWrapper[TargetingOne[O], O, R, TargetingOne[R]]
	{
		override protected def self: TargetingOne[R] = this
		
		override protected def wrapResult(result: O): R = f(result)
		override protected def wrap(newTarget: TargetingOne[O]): TargetingOne[R] = new Wrapper(newTarget, f)
	}
	
	private class Head[T <: TargetingManyLike[A, T, _], A](override val wrapped: T)
		extends TargetingOne[Option[A]] with TargetingWrapper[T, Seq[A], Seq[Value], Option[A], Value, TargetingOne[Option[A]]]
	{
		override protected def self: TargetingOne[Option[A]] = this
		
		override protected def wrapResult(result: Seq[A]): Option[A] = result.headOption
		override protected def wrapValue(value: Seq[Value]): Value = value.headOption.getOrElse(Value.empty)
		
		override protected def wrap(newTarget: T): TargetingOne[Option[A]] = new Head[T, A](newTarget)
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
