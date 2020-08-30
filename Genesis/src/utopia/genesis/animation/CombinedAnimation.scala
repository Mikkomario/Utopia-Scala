package utopia.genesis.animation

/**
  * This animation first plays the first animation and then the second
  * @author Mikko Hilpinen
  * @since 15.6.2020, v2.3
  * @param first The first animation to play
  * @param second The second animation to play
  * @param switchPoint The progress point at which the switch between the animations is made ]0, 1[
  * @tparam A Type of animation result
  */
case class CombinedAnimation[+A](first: Animation[A], second: Animation[A], switchPoint: Double = 0.5) extends Animation[A]
{
	override def apply(progress: Double) =
	{
		// Determines which animation piece to use
		if (progress < switchPoint)
			first(progress / switchPoint)
		else
			second((progress - switchPoint) / (1 - switchPoint))
	}
}
