package utopia.echo.controller.chat

import utopia.echo.model.request.ChatParams
import utopia.flow.async.TryFuture

import scala.concurrent.Future
import scala.util.Try

/**
 * A chat request executor that fails every time.
 * Useful as a placeholder for situations where no functioning executor may be created.
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
class AlwaysFailingChatRequestExecutor(failure: Throwable = new UnsupportedOperationException("Chat requests can't succeed"))
	extends BufferingChatRequestExecutor[Nothing]
{
	override def apply(params: ChatParams): Future[Try[Nothing]] = TryFuture.failure(failure)
}
