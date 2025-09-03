package utopia.echo.model.response.ollama

/**
 * @author Mikko Hilpinen
 * @since 02.09.2025, v1.4
 */
package object chat
{
	@deprecated("Please use OllamaReply instead", "v1.4")
	type ReplyMessage = OllamaReply
	@deprecated("Please use BufferedOllamaReply instead", "v1.4")
	type BufferedReplyMessage = BufferedOllamaReply
	@deprecated("Please use StreamedOllamaReply instead", "v1.4")
	type StreamedReplyMessage = StreamedOllamaReply
	@deprecated("Please use StreamedOrBufferedOllamaReply instead", "v1.4")
	type StreamedOrBufferedReplyMessage = StreamedOrBufferedOllamaReply
}
