package utopia.paradigm.animation

/**
  * Wraps a static instance as an animation
  * @author Mikko Hilpinen
  * @since Genesis 28.3.2020, v2.1
  */
case class StaticAnimation[+A](wrapped: A) extends Animation[A]
{
	override def apply(progress: Double) = wrapped
}
