package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.InstanceStatus
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase

/**
 * Represents a status snapshot of a Vast AI + vLLM processor pool
 * @param maxContextSize Maximum context size of this processor pool
 * @param maxParallelRequests Maximum number of parallel requests per client
 * @param instanceStates Status of each active processor. Each entry contains:
 *                          1. The current API-hosting phase
 *                          1. The current state of the utilized Vast AI instance, if applicable
 *                          1. Number of requests being processed (including queued)
 * @param requestsQueued Number of requests queued, waiting for available Vast AI + vLLM processors
 * @author Mikko Hilpinen
 * @since 04.03.2026, v1.5
 */
case class VastAiVllmProcessorPoolStatus(maxContextSize: Int, maxParallelRequests: Int,
                                         instanceStates: Seq[(VastAiVllmProcessPhase, Option[InstanceStatus], Int)],
                                         requestsQueued: Int)
