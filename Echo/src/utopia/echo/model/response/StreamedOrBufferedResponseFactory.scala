package utopia.echo.model.response

import scala.language.implicitConversions

/**
  * Common trait for factory / companion objects which construct streamed or buffered responses
  * @tparam Streamed Type of streamed responses
  * @tparam Buffered Type of buffered responses
  * @tparam StreamedOrBuffered Type of streamed or buffered response wrappers
  * @author Mikko Hilpinen
  * @since 21.07.2024, v1.0
  */
trait StreamedOrBufferedResponseFactory[-Streamed, -Buffered, +StreamedOrBuffered]
{
	// ABSTRACT ------------------------
	
	/**
	  * @param format Format in which the response is acquired.
	  *               Either streamed (Left) or buffered (Right).
	  * @return A streamed or buffered response wrapper, wrapping the specified response
	  */
	def apply(format: Either[Streamed, Buffered]): StreamedOrBuffered
	
	
	// IMPLICIT ------------------------
	
	/**
	  * Implicitly wraps a streamed response
	  * @param streamedResponse A streamed response to wrap
	  * @return Wrapped response
	  */
	implicit def streamed(streamedResponse: Streamed): StreamedOrBuffered = apply(Left(streamedResponse))
	/**
	  * Implicitly wraps a buffered response
	  * @param bufferedResponse A buffered response to wrap
	  * @return Wrapped response
	  */
	implicit def buffered(bufferedResponse: Buffered): StreamedOrBuffered = apply(Right(bufferedResponse))
}
