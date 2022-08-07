package utopia.paradigm.animation

/**
  * A static animation that won't change
  * @author Mikko Hilpinen
  * @since Genesis 18.4.2021, v2.5
  */
case class FixedAnimation[+A](value: A) extends AnimationLike[A, FixedAnimation]
{
	override def apply(progress: Double) = value
	
	override def reversed = this
	
	override def curved(curvature: AnimationLike[Double, Any]) = this
	
	override def map[B](f: A => B) = FixedAnimation(f(value))
	
	override def repeated(times: Int) = this
}
