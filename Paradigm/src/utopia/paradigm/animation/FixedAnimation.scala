package utopia.paradigm.animation
import scala.concurrent.duration.Duration

/**
  * A static animation that won't change
  * @author Mikko Hilpinen
  * @since Genesis 18.4.2021, v2.5
  */
case class FixedAnimation[+A](value: A) extends Animation[A] with AnimationLike[A, FixedAnimation]
{
	override def reversed = this
	
	override def apply(progress: Double) = value
	
	override def map[B](f: A => B) = FixedAnimation(f(value))
	
	override def curved(curvature: AnimationLike[Double, Any]) = this
	override def repeated(times: Int) = this
	
	override def withReverseAppended = this
	
	override def over(duration: Duration) = TimedAnimation.fixed(value, duration)
}
