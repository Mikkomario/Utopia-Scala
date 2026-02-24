package utopia.echo.controller.chat

import scala.concurrent.Future
import scala.language.implicitConversions
import scala.util.Try

object BufferedReplyGenerator
{
	// IMPLICIT ----------------------
	
	implicit def apply[R](f: String => Future[Try[R]]): BufferedReplyGenerator[R] = new _BufferedReplyGenerator[R](f)
	
	
	// NESTED   ----------------------
	
	private class _BufferedReplyGenerator[+R](f: String => Future[Try[R]]) extends BufferedReplyGenerator[R]
	{
		override def bufferedReplyFor(message: String): Future[Try[R]] = f(message)
	}
}

/**
 * A common interface for chat interfaces that convert prompts into responses in a non-streaming manner
 * @tparam R Type of the generated replies
 * @author Mikko Hilpinen
 * @since 23.02.2026, v1.4.1
 */
trait BufferedReplyGenerator[+R]
{
	// ABSTRACT --------------------------
	
	/**
	 * @param message A message / prompt to send
	 * @return A future that yields the reply, if successful
	 */
	def bufferedReplyFor(message: String): Future[Try[R]]
}
