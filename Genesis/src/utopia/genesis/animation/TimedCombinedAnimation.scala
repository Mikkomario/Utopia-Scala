package utopia.genesis.animation

/**
  * This animation combines two timed animations and plays them back to back
  * @author Mikko Hilpinen
  * @since 15.6.2020, v2.3
  * @param first The first animation that is played
  * @param second The second animation that is played
  */
case class TimedCombinedAnimation[+A](first: TimedAnimation[A], second: TimedAnimation[A]) extends TimedAnimation[A]
{
	// ATTRIBUTES	-------------------------
	
	private lazy val switchPoint = first.duration / duration
	
	override lazy val duration = first.duration + second.duration
	
	override def apply(progress: Double) =
	{
		if (progress < switchPoint)
			first(progress / switchPoint)
		else
			second((progress - switchPoint) / (1 - switchPoint))
	}
}
