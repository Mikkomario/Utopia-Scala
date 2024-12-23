package utopia.logos.model.cached

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.OptimizedIndexedSeq
import utopia.flow.parse.string.Regex
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.logos.model.stored.text.Delimiter
import utopia.logos.model.stored.url.StoredLink

import scala.collection.immutable.VectorBuilder

object Statement
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
					case Left(text) =>
						wordSplitRegex.split(text).view.map { _.trim }.filter { _.nonEmpty }.map { _ -> _word }
					case Right(delimiter) => delimiter.notEmpty.map { _ -> _delimiter }
				}
			case Right(link) => Some(link -> _link)
		}
		
		// Groups words and links to delimiter-separated groups
		val statementsBuilder = OptimizedIndexedSeq.newBuilder[Statement]
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
								Some(WordOrLink.link(str.notEndingWith("/")))
							// Case: Word or words => Splits into separate words
							else
								wordSplitRegex.split(str).filter { _.nonEmpty }
									// Cuts very long words
									.map { word => if (word.length > maxWordLength) s"${word.take(18)}..." else word }
									.map(WordOrLink.word)
						}
					
					statementsBuilder += apply(wordAndLinkParts, delimiterText)
					nextStartIndex = delimiterStartIndex + delimiterParts.size
				// Case: No delimiter found => Extracts text part without delimiter
				case None =>
					statementsBuilder += apply(
						parts.drop(nextStartIndex).map { case (str, role) => WordOrLink(str, isLink = role == _link) })
					nextStartIndex = parts.size
			}
		}
		statementsBuilder.result()
	}
}

/**
 * Represents a statement in text format (i.e. not id-based but text-based)
 * @author Mikko Hilpinen
 * @since 11/03/2024, v0.2
 */
case class Statement(words: Seq[WordOrLink], delimiter: String = "")
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
	
	
	// OTHER    -------------------------
	
	/**
	 * Mutates the text present in the individual words of this statement
	 * @param f A word text mutating function
	 * @return A mutated copy of this statement text
	 */
	def mapWordText(f: Mutate[String]) = copy(words = words.map { _.mapText(f) }.filterNot { _.text.isEmpty })
}
