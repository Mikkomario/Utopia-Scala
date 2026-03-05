package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.InstanceStatus
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.ApiHosting

import java.time.Instant

/**
 * Represents a status snapshot of a Vast AI + vLLM processor pool
 * @param maxContextSize Maximum context size of this processor pool
 * @param maxParallelRequestsPerClient Maximum number of parallel requests per client
 * @param instanceStates Status of each active processor. Each entry contains:
 *                          1. The current API-hosting phase
 *                          1. The current state of the utilized Vast AI instance, if applicable
 *                          1. Number of requests being processed (including queued)
 *                          1. Time when this processor was activated
 * @param requestsQueued Number of requests queued, waiting for available Vast AI + vLLM processors
 * @author Mikko Hilpinen
 * @since 04.03.2026, v1.5
 */
case class VastAiVllmProcessorPoolStatus(maxContextSize: Int, maxParallelRequestsPerClient: Int,
                                         instanceStates: Seq[(VastAiVllmProcessPhase, Option[InstanceStatus], Int, Instant)],
                                         requestsQueued: Int)
{
	// ATTRIBUTES   --------------------
	
	/**
	 * The current parallel processing capacity (i.e. the current maximum number of parallel requests)
	 */
	lazy val parallelCapacity = instanceStates.count { _._1 == ApiHosting } * maxParallelRequestsPerClient
	/**
	 * The current number of pending / queued requests, including those currently being processed
	 */
	lazy val pending = instanceStates.iterator.map { _._3 }.sum + requestsQueued
	/**
	 * The current number of requests being actively processed
	 */
	lazy val processing = instanceStates.iterator.map { _._3 min maxParallelRequestsPerClient }.sum
	
	
	// COMPUTED -----------------------
	
	/**
	 * @return The current number of requests waiting for processing
	 */
	def waiting = pending - processing
}