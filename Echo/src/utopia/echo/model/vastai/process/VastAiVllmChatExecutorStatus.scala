package utopia.echo.model.vastai.process

/**
 * Represents a status snapshot of a Vast AI + vLLM processor / chat executor
 * @param processorStates Status of each active processor
 * @param requestsQueued Number of requests queued, waiting for available Vast AI + vLLM processors
 * @author Mikko Hilpinen
 * @since 19.03.2026, v1.6
 */
case class VastAiVllmChatExecutorStatus(processorStates: Seq[VastAiVllmProcessorStatus], requestsQueued: Int)
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
}