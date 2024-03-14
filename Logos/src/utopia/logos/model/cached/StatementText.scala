package utopia.logos.model.cached

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.logos.model.stored.text.Delimiter
import utopia.logos.model.stored.url.Link

import scala.collection.immutable.VectorBuilder

object StatementText
{
	// ATTRIBUTES   -------------------------
	
	private val maxWordLength = 64
	
	// Enumeration for different statement elements
	private val _link = 1
	private val _delimiter = 2
	private val _word = 3
	
	private lazy val wordSplitRegex = Regex.whiteSpace || Regex.escape(' ') || Regex.newLine
	
	
	// OTHER    ----------------------------
	
	/**
	 * Finds all statements made within a text
	 * @param text A text
	 * @return All statements made within that text
	 */
	def allFrom(text: String) = {
		// Separates between links, delimiters and words
		val parts = Link.regex.divide(text).flatMap {
			case Left(text) =>
				// Trims words and filters out empty strings
				Delimiter.anyDelimiterRegex.divide(text).flatMap {
					case Left(text) => text.trim.notEmpty.map { _ -> _word }
					case Right(delimiter) => delimiter.notEmpty.map { _ -> _delimiter }
				}
			case Right(link) => Some(link -> _link)
		}
		
		// Groups words and links to delimiter-separated groups
		val statementsBuilder = new VectorBuilder[StatementText]()
		var nextStartIndex = 0
		while (nextStartIndex < parts.size) {
			// Finds the next delimiter
			(nextStartIndex until parts.size).find { parts(_)._2 == _delimiter } match {
				// Case: Next delimiter found => Collects remaining delimiter and extracts text part
				case Some(delimiterStartIndex) =>
					val delimiterParts = parts.drop(delimiterStartIndex).takeWhile { _._2 == _delimiter }.map { _._1 }
					val delimiterText = delimiterParts.mkString
					val wordAndLinkParts = parts.slice(nextStartIndex, delimiterStartIndex)
						.flatMap { case (str, role) =>
							// Case: Link => Removes the terminating /-character, if present
							if (role == _link)
								Some(WordOrLinkText.link(str.notEndingWith("/")))
							// Case: Word or words => Splits into separate words
							else
								wordSplitRegex.split(str).filter { _.nonEmpty }
									// Cuts very long words
									.map { word => if (word.length > maxWordLength) s"${word.take(18)}..." else word }
									.map(WordOrLinkText.word)
						}
					
					statementsBuilder += apply(wordAndLinkParts, delimiterText)
					nextStartIndex = delimiterStartIndex + delimiterParts.size
				// Case: No delimiter found => Extracts text part without delimiter
				case None =>
					statementsBuilder += apply(
						parts.drop(nextStartIndex).map { case (str, role) => WordOrLinkText(str, isLink = role == _link) })
					nextStartIndex = parts.size
			}
		}
		statementsBuilder.result()
	}
}

/**
 * Represents a statement in text format (i.e. not id-based but text-based)
 * @author Mikko Hilpinen
 * @since 11/03/2024, v1.0
 */
case class StatementText(words: Vector[WordOrLinkText], delimiter: String = "")
{
	// ATTRIBUTES   -----------------------
	
	override lazy val toString = s"${words.mkString(" ")}$delimiter"
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @return A pair containing:
	 *              1) The words within this statement, and
	 *              2) The links within this statement
	 */
	def wordsAndLinks = words.divideBy { _.isLink }.map { _.map { _.text } }
}
