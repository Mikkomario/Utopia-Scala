package utopia.disciple.controller.parse

import utopia.access.model.Headers
import utopia.access.model.enumeration.Status
import utopia.flow.parse.file.FileExtensions._

import java.io.InputStream
import java.nio.file.Path
import scala.util.{Failure, Success, Try}

object WriteToFileResponseParser
{
	/**
	 * Creates a response parser that writes all (non-empty) responses to a file
	 * @param path A function that yields the file to write.
	 *             Called once for each non-empty response.
	 * @return A new response parser
	 */
	def to(path: => Path) = apply { (_, _) => path.createParentDirectories() }
	
	/**
	 * Creates a new response parser that writes responses to local files.
	 * @param determinePath A function that receives the response status + headers,
	 *                      and yields the path to which the response body (which is non-empty) should be written.
	 *                      Yields a failure if this parser should yield a failure instead of writing the response body.
	 * @return A new response parser
	 */
	def apply(determinePath: (Status, Headers) => Try[Path]) = new WriteToFileResponseParser(determinePath)
}

/**
 * A response parser that writes the streamed response contents into a local file.
 * Intended to be combined with a response parser that better handles failure responses.
 * @author Mikko Hilpinen
 * @since 28.09.2025, v1.9.1
 */
class WriteToFileResponseParser(determinePath: (Status, Headers) => Try[Path]) extends ResponseParser[Try[Path]]
{
	override def apply(status: Status, headers: Headers,
	                   stream: Option[InputStream]): ResponseParseResult[Try[Path]] =
	{
		stream match {
			// Case: Non-empty response => Determines the file to write
			case Some(stream) =>
				determinePath(status, headers) match {
					// Case: File determined => Proceeds to write
					case Success(path) => ResponseParseResult.buffered(path.writeStream(stream))
					// Case: Writing canceled => Closes the input stream and yields a failure
					case Failure(error) =>
						Try { stream.close() }
						ResponseParseResult.failed(error)
				}
			// Case: Empty response => Fails
			case None => ResponseParseResult.failed(new IllegalArgumentException("The response is empty"))
		}
	}
}
