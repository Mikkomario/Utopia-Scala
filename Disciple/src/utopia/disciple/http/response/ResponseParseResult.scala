package utopia.disciple.http.response

import utopia.flow.view.template.Extender
import utopia.flow.parse.AutoClose._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions
import scala.util.Failure

object ResponseParseResult
{
	// ATTRIBUTES   -------------------
	
	/**
	  * An empty, immediately resolved response parse result
	  */
	lazy val empty = buffered(())
	
	
	// IMPLICIT -----------------------
	
	/**
	  * @param future Asynchronous parse completion to wrap
	  * @tparam A Type of the parse results, once they become available
	  * @return Parse result wrapping the specified future.
	  *         Parsing is considered completed once the specified future resolves.
	  */
	implicit def future[A](future: Future[A]): ResponseParseResult[Future[A]] = apply(future, future)
	
	
	// OTHER    -----------------------
	
	/**
	  * Wraps an immediately available / buffered result
	  * @param value Value to wrap
	  * @tparam A Type of the wrapped value
	  * @return A parse result completed with the specified value
	  */
	def buffered[A](value: A) = apply(value, Future.successful(()))
	
	/**
	  * Completes response-parsing asynchronously.
	  * @param f A function which yields the parsing result. Called asynchronously.
	  * @param exc Implicit execution context.
	  * @tparam A Type of parse results, once they become available
	  * @return A parse result which completes asynchronously once the specified function returns
	  */
	def async[A](f: => A)(implicit exc: ExecutionContext) = future(Future(f))
	
	/**
	  * @param cause Cause of failure
	  * @tparam A Value type which would have been used had the result been successful
	  * @return A failed response parse result
	  */
	def failed[A](cause: Throwable) = buffered(Failure[A](cause))
}

/**
  * A wrapper for response-parsing results.
  * These wrap the parsed value (which may complete asynchronously) and indicate when the parsing has completed.
  * @author Mikko Hilpinen
  * @since 13.07.2024, v1.6.4
  */
case class ResponseParseResult[+A](wrapped: A, parseCompletion: Future[Any]) extends Extender[A]
{
	// COMPUTED ----------------------
	
	/**
	  * @return Whether the parsing has completed at this time
	  */
	def isCompleted = parseCompletion.isCompleted
	
	
	// OTHER    ----------------------
	
	/**
	  * Closes the specified instance once parsing has completed (which may be immediately)
	  * @param a Item to close
	  * @param exc Implicit execution context used for scheduling the closing operation in case of an asynchronous
	  *            parsing completion.
	  */
	def closeOnCompletion(a: AutoCloseable)(implicit exc: ExecutionContext): Unit = {
		if (isCompleted)
			a.closeQuietly()
		else
			parseCompletion.onComplete { _ => a.closeQuietly() }
	}
	
	/**
	  * Creates a new parse result by mapping this result
	  * @param f Mapping function applied to the wrapped value
	  * @tparam B Type of mapping results
	  * @return Copy of this result with a mapped value
	  */
	def map[B](f: A => B) = ResponseParseResult(f(wrapped), parseCompletion)
}