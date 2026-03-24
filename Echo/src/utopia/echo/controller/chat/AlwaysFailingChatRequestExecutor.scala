package utopia.echo.controller.chat

import utopia.annex.model.response.RequestNotSent.RequestSendingFailed
import utopia.annex.model.response.RequestResult
import utopia.echo.model.request.ChatParams

import scala.concurrent.Future

/**
 * A chat request executor that fails every time.
 * Useful as a placeholder for situations where no functioning executor may be created.
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
class AlwaysFailingChatRequestExecutor(failure: Throwable = new UnsupportedOperationException("Chat requests can't succeed"))
	extends BufferingChatRequestExecutor[Nothing]
{
	override def apply(params: ChatParams): Future[RequestResult[Nothing]] =
		Future.successful(RequestSendingFailed(failure))
}
