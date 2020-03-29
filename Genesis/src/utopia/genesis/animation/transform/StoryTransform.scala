package utopia.genesis.animation.transform

import scala.concurrent.duration.Duration
import utopia.flow.util.CollectionExtensions._

/**
  * Used for transforming an instance using possibly multiple timelines and layers of transformations
  * @author Mikko Hilpinen
  * @since 11.8.2019, v2.1+
  */
case class StoryTransform[A](timelines: Seq[TimelineTransform[A, A]]) extends TimedTransform[A, A]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * The start time of this story
	  */
	lazy val start = timelines.map { _.delay }.minOption.getOrElse(Duration.Undefined)
	/**
	  * The end time of this story
	  */
	lazy val end = timelines.map { _.duration }.maxOption.getOrElse(Duration.Undefined)
	
	
	// IMPLEMENTED	--------------------
	
	override def duration = end
	
	override def apply(original: A, progress: Double): A = apply(original, start + (end - start) * progress)
	
	
	// OTHER	------------------------
	
	/**
	  * Transforms the item based on a specific situation in this story
	  * @param original The original item
	  * @param passedTime A timestamp inside or outside this story
	  * @return Transformed version of the item, based on active timeline(s)
	  */
	override def apply(original: A, passedTime: Duration) = timelines.foldLeft(original) { (current, timeline) =>
		timeline(current, passedTime) getOrElse current }
}
