package utopia.echo.model.response.ollama

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.Alive
import utopia.bunnymunch.jawn.JsonBunny
import utopia.echo.model.error.ParseException
import utopia.echo.model.response.ollama.BufferedOllamaResponseLike.{escapedEscapeRegex, invalidArrayRegex, lineCommentRegex}
import utopia.flow.async.TryFuture
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.flow.util.TryExtensions._
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

object BufferedOllamaResponseLike
{
	// ATTRIBUTES   ------------------------
	
	private lazy val invalidArrayRegex =
		Regex.escape(',') + (Regex.whiteSpace || Regex.newLine).anyTimes.withinParentheses + Regex.escape(']')
	private lazy val lineCommentRegex =
		Regex.escape('/').times(2) + (!Regex.newLine.anyTimes).withinParentheses + Regex.newLine
	private lazy val escapedEscapeRegex = Regex.escape('\\').times(2)
}

/**
 * Common trait for Ollama responses which have been fully received & buffered
 * @tparam Repr Implementing type of this trait
 * @author Mikko Hilpinen
 * @since 12.01.2025, v1.2
 */
trait BufferedOllamaResponseLike[+Repr] extends OllamaResponseLike[Repr]
{
	// ABSTRACT ------------------------
	
	/**
	 * @return This response
	 */
	def self: Repr
	/**
	 * @return Statistics concerning this response
	 */
	def statistics: ResponseStatistics
	/**
	  * @return The reflective content produced by the LLM before the final answer.
	  *         May be empty.
	  */
	def thoughts: String
	
	
	// COMPUTED ------------------------
	
	/**
	 * Finds and parses json contents from a completed response.
	 * Assumes that the reply is requested as a single json array.
	 * Supports situations where the response contains other text, also.
	 * @return A json array parsed from the contents of this response.
	 */
	def jsonArray = closedJsonEntity('[', ']', "json array").flatMap { _.tryVector }
	/**
	 * Finds and parses json contents from a completed response.
	 * Assumes that the reply is requested as a single json object.
	 * Supports situations where the response contains other text, also.
	 * @return A json object parsed from the contents of this response.
	 */
	def jsonObject = closedJsonEntity('{', '}', "json object").flatMap { _.tryModel }
	
	
	// IMPLEMENTED  --------------------
	
	override def isBuffered: Boolean = true
	
	override def future: Future[Try[Repr]] = TryFuture.success(self)
	override def statisticsFuture: Future[Try[ResponseStatistics]] = TryFuture.success(statistics)
	
	override def state: SchrodingerState = Alive
	
	override def textPointer: Changing[String] = Fixed(text)
	override def newTextPointer: Changing[String] = Fixed(text)
	override def lastUpdatedPointer: Changing[Instant] = Fixed(lastUpdated)
	
	
	// OTHER    ------------------------
	
	private def closedJsonEntity(startChar: Char, endChar: Char, searchedEntityName: => String) = {
		val text = this.text
		Some(text.indexOf(startChar)).filter { _ >= 0 }
			.toTry { new NoSuchElementException(s"No $searchedEntityName is present in '$text'") }
			.flatMap { start =>
				Some(text.lastIndexOf(endChar)).filter { _ > start }
					.toTry { new NoSuchElementException(s"No complete $searchedEntityName is present in '$text'") }
					.flatMap { end =>
						// It's possible that the LLM escaped an escape sign. We'll convert these back to normal.
						val parsedPart = text.substring(start, end + 1).replaceEachMatchOf(escapedEscapeRegex, "\\")
						val defaultResult = JsonBunny.munch(parsedPart)
						// Sometimes the parsing fails because of ",]" text entries.
						// Attempts to remove these, just in case.
						// Removes comments as well
						defaultResult.orElse {
							val fixedStr = parsedPart
								.replaceEachMatchOf(invalidArrayRegex, "]").replaceEachMatchOf(lineCommentRegex, "\n")
							if (fixedStr.length == parsedPart.length)
								defaultResult.mapFailure { new ParseException(s"Failed to parse '$text'", _) }
							else
								JsonBunny.munch(fixedStr)
									.mapFailure { new ParseException(s"Failed to parse '$fixedStr'", _) }
						}
					}
			}
	}
}
