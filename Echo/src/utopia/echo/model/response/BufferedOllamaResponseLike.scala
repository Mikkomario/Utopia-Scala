package utopia.echo.model.response

import utopia.annex.model.manifest.SchrodingerState
import utopia.annex.model.manifest.SchrodingerState.Alive
import utopia.bunnymunch.jawn.JsonBunny
import utopia.echo.model.response.BufferedOllamaResponseLike.arrayStartRegex
import utopia.flow.async.TryFuture
import utopia.flow.parse.string.Regex
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.flow.util.TryExtensions._
import utopia.flow.collection.CollectionExtensions._

import java.time.Instant
import scala.concurrent.Future
import scala.util.Try

object BufferedOllamaResponseLike
{
	private lazy val arrayStartRegex = Regex.escape('[')
	private lazy val arrayEndRegex = Regex.escape(']')
	
	private lazy val objectStartRegex = Regex.escape('{')
	private lazy val objectEndRegex = Regex.escape('}')
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
	
	private def closedJsonEntity(startChar: Char, endChar: Char, searchedEntityName: => String) =
		Some(text.indexOf(startChar)).filter { _ >= 0 }
			.toTry { new NoSuchElementException(s"No $searchedEntityName is present in '$text'") }
			.flatMap { start =>
				Some(text.lastIndexOf(endChar)).filter { _ > start }
					.toTry { new NoSuchElementException(s"No complete $searchedEntityName is present in '$text'") }
					.flatMap { end => JsonBunny.munch(text.substring(start, end + 1)) }
			}
}
