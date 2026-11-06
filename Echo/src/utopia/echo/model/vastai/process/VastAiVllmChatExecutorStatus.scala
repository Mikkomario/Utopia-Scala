package utopia.echo.model.vastai.process

import utopia.echo.model.tokenization.RequestCount
import utopia.echo.model.vastai.process.VastAiServiceState.VastAiServicePhase.{NotStarted, Serving}
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
	 * The combined number of pending requests and tokens (input + max output).
	 * Note: Doesn't include [[requestsQueued]], as those are not measured in tokens.
	 */
	lazy val pending = sumRequests { _.pending }
	/**
	 * The combined number of requests and tokens (input + max output) being actively processed
	 */
	lazy val processing = sumRequests { _.active }
	
	
	// IMPLEMENTED  -------------------
	
	override def toModel: Model = {
		val phase = {
			if (processorStates.isEmpty)
				NotStarted
			else {
				val phases = processorStates.map { _.phase }
				if (phases.contains(Serving))
					Serving
				else
					phases.iterator.filter { _ < Serving }.maxOption.getOrElse { phases.min }
			}
		}
		Model.from(
			"phase" -> Model.from("name" -> phase.name, "index" -> phase.index),
			"processors" -> processorStates, "queued" -> requestsQueued,
			"pendingTotal" -> pending, "activeTotal" -> processing)
	}
	
	
	// OTHER    -----------------------
	
	private def sumRequests(getRequests: VastAiVllmProcessorStatus => RequestCount) =
		processorStates.iterator.map(getRequests).reduceOption { _ + _ }.getOrElse { RequestCount.zero }
}