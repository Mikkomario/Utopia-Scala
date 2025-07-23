package utopia.logos.model.cached

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq, Pair, Single}
import utopia.flow.parse.string.Regex
import utopia.flow.util.Mutate
import utopia.flow.util.StringExtensions._
import utopia.logos.model.stored.text.Delimiter

object Statement
{
	// ATTRIBUTES   -------------------------
	
	private val maxWordLength = 64
	
	// Enumeration for different statement elements
	private val _link = 1
	private val _delimiter = 2
	private val _word = 3
	
	/**
	 * A regular expression for separating words from each other
	 */
	lazy val wordSplitRegex = Regex.whiteSpace || Regex.escape(' ') || Regex.newLine
	/**
	 * These characters may appear within a link, but if they appear at the end,
	 * should be considered delimiters instead.
	 */
	private lazy val linkEndingDelimiterChars = Set(')', ']', ',', '.', '?', ':', ';', '\'')
	
	/**
	 * An empty statement
	 */
	lazy val empty = apply(Empty)
	
	
	// OTHER    ----------------------------
	
	/**
	 * Converts a statement string into a statement instance.
	 * Only expects a singular statement. Won't split into multiple, even if delimiters are found.
	 * @param statement A statement string
	 * @return A statement from the specified string
	 */
	def singleFrom(statement: String) = {
		if (statement.isEmpty)
			empty
		else {
			// Checks for links
			val parts = Link.regex.divide(statement)
			val (lastPart, delimiter) = parts.last match {
				case Left(text) =>
					val (textPart, delimiter) = Delimiter.regex.any.rangesIteratorIn(text)
						.lastOption.filter { _.end == text.length } match
					{
						case Some(delimiterRange) =>
							text.take(delimiterRange.start) -> text.slice(delimiterRange)
						case None => text -> ""
					}
					textPart.split(wordSplitRegex).filter { _.nonEmpty }.map(WordOrLink.word) -> delimiter
				
				case Right(link) => WordOrLink.link(link).emptyOrSingle -> ""
			}
			val otherParts = parts.dropRight(1).flatMap {
				case Left(text) => text.split(wordSplitRegex).filter { _.nonEmpty }.map(WordOrLink.word)
				case Right(link) => WordOrLink.link(link).emptyOrSingle
			}
			apply(otherParts ++ lastPart, delimiter)
		}
	}
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
				Delimiter.regex.any.divide(text).flatMap {
					case Left(text) =>
						wordSplitRegex.split(text).view.map { _.trim }.filter { _.nonEmpty }.map { _ -> _word }
						
					case Right(delimiter) => delimiter.notEmpty.map { _ -> _delimiter }
				}
			case Right(link) =>
				// May extract certain delimiter characters from the end of the link path
				val delimiterPart = link.takeRightWhile(linkEndingDelimiterChars.contains)
				if (delimiterPart.nonEmpty)
					Pair(link.dropRight(delimiterPart.length) -> _link, delimiterPart -> _delimiter)
				else
					Single(link -> _link)
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
								WordOrLink.link(str.notEndingWith("/"))
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
					val lastParts = parts.view.drop(nextStartIndex)
						.flatMap { case (str, role) =>
							if (role == _link)
								WordOrLink.link(str.notEndingWith("/"))
							else if (str.length > maxWordLength)
								Some(WordOrLink.word(s"${str.take(18)}..."))
							else
								Some(WordOrLink.word(str))
						}
						.toOptimizedSeq
					statementsBuilder += apply(lastParts)
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
	
	/**
	 * Words that appear within this statement, in their standard forms, coupled with their local display styles
	 */
	lazy val standardizedWords =
		words.flatMap { word => if (word.isWord) Some(word.standardizedText -> word.style) else None }
	/**
	 * Links that appear within this statement, as text
	 */
	lazy val links = words.flatMap { _.link }
	
	
	// OTHER    -------------------------
	
	/**
	 * Mutates the text present in the individual words of this statement
	 * @param f A word text mutating function
	 * @return A mutated copy of this statement text
	 */
	def mapWordText(f: Mutate[String]) =
		copy(words = words.view.map { _.mapIfWord(f) }.filter { _.nonEmpty }.toOptimizedSeq)
}
