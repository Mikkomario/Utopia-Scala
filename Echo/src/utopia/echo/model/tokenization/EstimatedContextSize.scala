package utopia.echo.model.tokenization

/**
 * Combines multiple token calculation values.
 * Used for preparing context & response sizes when sending a request.
 * @author Mikko Hilpinen
 * @since 23.03.2025, v1.3
 * @param newMessages The number of estimated tokens within the new messages in the prompt
 * @param systemAndHistory Number of tokens present in the message history & system message(s).
 *                         May be a full or a partial estimate.
 * @param context Reserved context size as a number of tokens
 */
case class EstimatedContextSize(newMessages: EstimatedTokenCount, systemAndHistory: PartiallyEstimatedTokenCount,
                                context: TokenCount)
{
	/**
	 * Maximum allowed size for the response (in tokens).
	 * Based on the available context size, minus used capacity. Set to not overflow the context size.
	 */
	lazy val maxResponse = context - systemAndHistory - newMessages
}