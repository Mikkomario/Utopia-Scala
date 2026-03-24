package utopia.echo.model.vastai.process

import utopia.echo.model.tokenization.TokenCount
import utopia.echo.model.vastai.instance.InstanceStatus
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase
import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.ApiHosting
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

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
                                     activeTokens: TokenCount, pendingTokens: TokenCount, maxContextSize: TokenCount,
                                     started: Instant, lastRequestTime: Instant, lastPendingStarted: Instant,
                                     lastPendingEnded: Instant)
	extends ModelConvertible
{
	override def toModel: Model = Model.from("maxContext" -> maxContextSize.value,
		"phase" -> Model.from("name" -> phase.name, "index" -> phase.index), "instanceStatus" -> instanceStatus,
		"usable" -> (phase == ApiHosting),
		"tokenUsage" -> Model.from("active" -> activeTokens.value, "pending" -> pendingTokens.value),
		"started" -> started, "lastRequestTime" -> lastRequestTime, "lastPendingStarted" -> lastPendingStarted,
		"lastPendingEnded" -> lastPendingEnded)
}
