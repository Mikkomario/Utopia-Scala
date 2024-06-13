package utopia.flow.test

import utopia.flow.async.process.Wait
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.ProgressTracker
import TestContext._

/**
 * A simple test for progress-tracking
 *
 * @author Mikko Hilpinen
 * @since 07.05.2024, v2.4
 */
object ProgressTrackingTest extends App
{
	private val tracker = ProgressTracker(0) { _ / 10.0 }
	tracker.addListener { e =>
		println(s"${ e.value }/10 - ${ (e.currentProgress * 100).toInt }% - ${
			e.projectedRemainingDuration.description } remaining")
	}
	
	println(s"Updating progress, ETA completion in 10 sec")
	(1 to 10).foreach { i =>
		Wait(1.seconds)
		tracker.value = i
	}
	
	println("Done!")
}
