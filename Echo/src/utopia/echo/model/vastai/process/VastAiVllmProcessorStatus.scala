package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.InstanceStatus
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase

import java.time.Instant

/**
 * Represents the status of a single Vast AI instance -based request-processor
 * @param phase Current process phase
 * @param instanceStatus Status of the managed Vast AI instance, if applicable
 * @param activeTokens Number of tokens (input + max output) currently being processed
 * @param pendingTokens Number of tokens (input + max output) waiting for processing
 * @param maxContextSize Maximum context size on this instance
 * @param started Time when this process was started (i.e. when the instance was being acquired)
 * @author Mikko Hilpinen
 * @since 19.03.2026, v1.6
 */
case class VastAiVllmProcessorStatus(phase: VastAiVllmProcessPhase, instanceStatus: Option[InstanceStatus],
                                     activeTokens: Int, pendingTokens: Int, maxContextSize: Int, started: Instant,
                                     lastRequestTime: Instant, lastPendingStarted: Instant, lastPendingEnded: Instant)
