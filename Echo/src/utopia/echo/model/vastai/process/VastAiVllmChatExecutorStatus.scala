package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.process.VastAiVllmProcessState.VastAiVllmProcessPhase.{ApiHosting, NotStarted}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.generic.model.template.ModelConvertible

/**
 * Represents a status snapshot of a Vast AI + vLLM processor / chat executor
 * @param processorStates Status of each active processor
 * @param requestsQueued Number of requests queued, waiting for available Vast AI + vLLM processors
 * @author Mikko Hilpinen
 * @since 19.03.2026, v1.6
 */
case class VastAiVllmChatExecutorStatus(processorStates: Seq[VastAiVllmProcessorStatus], requestsQueued: Int)
	extends ModelConvertible
{
	// ATTRIBUTES   --------------------
	
	/**
	 * The combined amount of pending request tokens (input + max output).
	 * Note: Doesn't include [[requestsQueued]], as those are not measured in tokens.
	 */
	lazy val pending = processorStates.iterator.map { _.pendingTokens }.sum
	/**
	 * The combined amount of tokens (input + max output) being actively processed
	 */
	lazy val processing = processorStates.iterator.map { _.activeTokens }.sum
	
	
	// IMPLEMENTED  -------------------
	
	override def toModel: Model = {
		val phase = {
			if (processorStates.isEmpty)
				NotStarted
			else {
				val phases = processorStates.map { _.phase }
				if (phases.contains(ApiHosting))
					ApiHosting
				else
					phases.iterator.filter { _ < ApiHosting }.maxOption.getOrElse { phases.min }
			}
		}
		Model.from(
			"phase" -> Model.from("name" -> phase.name, "index" -> phase.index),
			"processors" -> processorStates, "queued" -> requestsQueued,
			"pending_tokens_total" -> pending.value, "active_tokens_total" -> processing.value)
	}
}