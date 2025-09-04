package utopia.echo.model.response

/**
 * Common trait / interface for LLM replies, whether they're streamed or buffered.
 * @author Mikko Hilpinen
 * @since 03.09.2025, v1.4
 */
trait Reply extends ReplyLike[BufferedReply]
