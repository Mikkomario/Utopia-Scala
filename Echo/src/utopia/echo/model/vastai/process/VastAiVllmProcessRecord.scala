package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.offer.Offer
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._

import java.time.Instant

/**
 * Records information about a past Vast AI + vLLM process
 * @param hostingResult Result of API hosting
 * @param started Time when the process was started
 * @param terminated Time when the process was fully terminated
 * @param loaded Time when the instance was fully loaded. None if it didn't fully load.
 * @param apiStarted Time when the API became usable. None if it didn't.
 * @param stopped Time when the process was requested to stop. None if no request to stop was received.
 * @param offer Selected offer. None if the process failed before an offer was selected
 *              (e.g. if no offers were available).
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
case class VastAiVllmProcessRecord(hostingResult: ApiHostingResult, started: Instant, terminated: Instant,
                                   loaded: Option[Instant] = None, apiStarted: Option[Instant] = None,
                                   stopped: Option[Instant] = None, offer: Option[Offer] = None)
{
	// ATTRIBUTES   --------------------------
	
	/**
	 * Duration how long the instance was being loaded
	 */
	lazy val loadDuration = loaded match {
		case Some(loaded) => loaded - started
		case None => stopped.getOrElse(terminated) - started
	}
	/**
	 * Duration how long the API was being set up
	 */
	lazy val setupDuration = apiStarted match {
		case Some(apiStarted) =>
			loaded match {
				case Some(loaded) => apiStarted - loaded
				case None => Duration.zero
			}
		case None => Duration.zero
	}
	/**
	 * Duration how long the API was hosted
	 */
	lazy val hostDuration = apiStarted match {
		case Some(hostingStarted) => stopped.getOrElse(terminated) - hostingStarted
		case None => Duration.zero
	}
	
	
	// COMPUTED -----------------------------
	
	/**
	 * @return Total process duration
	 */
	def totalDuration = terminated - started
	
	/**
	 * @return Whether the API was hosted for some time
	 */
	def hostedApi = hostingResult.wasHosted
}