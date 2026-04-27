package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.VastAiInstance

import java.time.Instant

/**
 * Common trait for interfaces that want to receive notifications of when Vast AI + vLLM instances become usable,
 * and when they're terminated.
 * @author Mikko Hilpinen
 * @since 26.03.2026, v1.6
 */
trait VastAiVllmProcessRecorder
{
	/**
	 * This method should be called whenever a vLLM API is successfully hosted
	 * @param instance Instance on which the API was hosted
	 * @param started Time when the instance was started
	 * @param loaded Time when the instance was fully loaded
	 */
	def onApiSetup(instance: VastAiInstance, started: Instant, loaded: Instant): Unit
	
	/**
	 * This method should be called whenever a Vast AI + vLLM process completes,
	 * whether successfully or because of a failure.
	 * @param record Record of the process that was run
	 */
	def onProcessCompleted(record: VastAiVllmProcessRecord): Unit
}
