package utopia.logos.database.access.many.text.statement

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.parse.string.Regex
import utopia.flow.util.StringExtensions._
import utopia.vault.database.Connection
import utopia.vault.nosql.view.UnconditionalView
import utopia.logos.database.access.many.text.delimiter.DbDelimiters
import utopia.logos.database.access.many.text.word.DbWords
import utopia.logos.database.access.many.url.link.DbLinks
import utopia.logos.database.access.single.text.statement.DbStatement
import utopia.logos.model.stored.text.Delimiter
import utopia.logos.model.stored.url.Link

import scala.collection.immutable.VectorBuilder

/**
  * The root access point when targeting multiple statements at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DbStatements extends ManyStatementsAccess with UnconditionalView
{
	// ATTRIBUTES   ----------------
	
	private val maxWordLength = 64
	
	// Enumeration for different statement elements
	private val _link = 1
	private val _delimiter = 2
	private val _word = 3
	
	private lazy val wordSplitRegex = Regex.whiteSpace || Regex.escape(' ') || Regex.newLine
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted statements
	  * @return An access point to statements with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbStatementsSubset(ids)
	
	/**
	 * Stores the specified text to the database as a sequence of statements.
	 * Avoids inserting duplicate entries.
	 * @param text Text to store as statements
	 * @param connection Implicit DB connection
	 * @return Stored statements, where each entry is either right, if it existed already, or left,
	 *         if it was newly inserted
	 */
	def store(text: String)(implicit connection: Connection) = {
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
		// [([(text, isLink)], delimiter)]
		val dataBuilder = new VectorBuilder[(Vector[(String, Boolean)], String)]()
		var nextStartIndex = 0
		while (nextStartIndex < parts.size) {
			// Finds the next delimiter
			(nextStartIndex until parts.size).find { parts(_)._2 == _delimiter } match {
				// Case: Next delimiter found => Collects remaining delimiter and extracts text part
				case Some(delimiterStartIndex) =>
					val delimiterParts = parts.drop(delimiterStartIndex).takeWhile { _._2 == _delimiter }.map { _._1 }
					val delimiterText = delimiterParts.mkString
					val wordAndLinkParts = parts.slice(nextStartIndex, delimiterStartIndex)
						.map { case (str, role) =>
							val isLink = role == _link
							// Removes trailing forward slashes from links
							val processedStr = {
								if (isLink)
									str.notEndingWith("/")
								else
									str
							}
							processedStr -> isLink
						}
					
					dataBuilder += wordAndLinkParts -> delimiterText
					nextStartIndex = delimiterStartIndex + delimiterParts.size
				// Case: No delimiter found => Extracts text part without delimiter
				case None =>
					dataBuilder += (parts.drop(nextStartIndex).map { case (str, role) => str -> (role == _link) } -> "")
					nextStartIndex = parts.size
			}
		}
		val data = dataBuilder.result()
		// Stores the delimiters first
		val delimiterMap = DbDelimiters.store(data.map { _._2 }.toSet.filterNot { _.isEmpty })
		// Next stores the words, the links and the statements
		val statementWordData = data.map { case (wordsPart, delimiterPart) =>
			val splitWordsPart = wordsPart.flatMap { case (text, isLink) =>
				if (isLink)
					Some(text -> isLink)
				else
					wordSplitRegex.split(text).filter { _.nonEmpty }
						// Cuts very long words
						.map { word =>
							if (word.length > maxWordLength)
								s"${word.take(18)}..." -> isLink
							else
								word -> isLink
						}
			}
			splitWordsPart -> delimiterMap.get(delimiterPart)
		}
		// False contains words, True contains links
		val wordsAndLinks = statementWordData.flatMap { _._1 }.groupMap { _._2 } { _._1 }
			.view.mapValues { _.toSet }.toMap
		val wordMap = DbWords.store(wordsAndLinks.getOrElse(false, Set[String]()))
		val linkMap = DbLinks.store(wordsAndLinks.getOrElse(true, Set[String]()))
			.merge { _ ++ _ }.map { l => l.toString.toLowerCase -> l.id }.toMap
		
		statementWordData.map { case (words, delimiterId) =>
			val wordIds = words.flatMap { case (word, isLink) =>
				val result = if (isLink) linkMap.get(word.toLowerCase) else wordMap.get(word)
				if (result.isEmpty)
					println(s"Warning: Failed to match $word with options: ${
						(if (isLink) linkMap else wordMap).keys.mkString(", ")}")
				result.map { _ -> isLink }
			}
			DbStatement.store(wordIds, delimiterId)
		}
	}
	
	
	// NESTED	--------------------
	
	class DbStatementsSubset(targetIds: Set[Int]) extends ManyStatementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

