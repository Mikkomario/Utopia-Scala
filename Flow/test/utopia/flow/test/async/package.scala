package utopia.flow.test

import utopia.flow.time.{Duration, Now}
import utopia.flow.time.TimeExtensions._

import java.time.Instant

/**
 * @author Mikko Hilpinen
 * @since 17.03.2026, v2.8.1
 */
package object async
{
	def testTime(phase: String, expectedMin: Duration, expectedMax: Duration)(implicit t0: Instant) = {
		val d = Now.toInstant - t0
		println(s"${ d.description }: $phase")
		assert(d >= expectedMin && d <= expectedMax)
	}
}
