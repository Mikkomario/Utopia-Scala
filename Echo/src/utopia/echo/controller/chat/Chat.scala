package utopia.echo.controller.chat

import utopia.echo.model.response.{BufferedReply, Reply}

/**
 * An interface for interactive chat which supports conversation history and tools.
 *
 * Note: While this interface supports request-queueing and other asynchronous processes,
 *       one must be careful when manually modifying the message history and/or system messages.
 *       It is safest to do so only once the previously queued requests have completed.
 *
 * @author Mikko Hilpinen
 * @since 16.09.2024, v1.1
 */
trait Chat extends ChatLike[Reply, BufferedReply, Chat]
