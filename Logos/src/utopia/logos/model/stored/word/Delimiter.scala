package utopia.logos.model.stored.word

import utopia.flow.parse.string.Regex
import utopia.logos.database.access.single.word.delimiter.DbSingleDelimiter
import utopia.logos.model.factory.word.DelimiterFactory
import utopia.logos.model.partial.word.DelimiterData
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

import java.time.Instant

/**
  * Represents a delimiter that has already been stored in the database
  * @param id id of this delimiter in the database
  * @param data Wrapped delimiter data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class Delimiter(id: Int, data: DelimiterData) 
	extends StoredModelConvertible[DelimiterData] with DelimiterFactory[Delimiter] with FromIdFactory[Int, Delimiter]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this delimiter in the database
	  */
	def access = DbSingleDelimiter(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def toString = data.text
	
	override def withCreated(created: Instant) = copy(data = data.withCreated(created))
	
	override def withId(id: Int) = copy(id = id)
	
	override def withText(text: String) = copy(data = data.withText(text))
}

object Delimiter
{
	// ATTRIBUTES	--------------------
	
	private lazy val commaRegex = Regex.escape(',')
	
	private lazy val periodRegex = Regex.escape('.')
	
	private lazy val startingParenthesisRegex = Regex.escape('(')
	
	private lazy val endingParenthesisRegex = Regex.escape(')')
	
	private lazy val exclamationRegex = Regex.escape('!')
	
	private lazy val questionRegex = Regex.escape('?')
	
	private lazy val colonRegex = Regex.escape(':')
	
	private lazy val dashRegex = Regex.escape('-')
	
	private lazy val quotationRegex = Regex("\\\"")
	
	private lazy val spacedDelimiterRegex = 
		((commaRegex || periodRegex || exclamationRegex || questionRegex ||
			 colonRegex || endingParenthesisRegex).withinParenthesis.oneOrMoreTimes +(Regex.whiteSpace || Regex.endOfString || Regex.newLine).withinParenthesis).withinParenthesis
	
	private lazy val surroundedDashRegex = (Regex.whiteSpace + dashRegex + Regex.whiteSpace).withinParenthesis
	
	/**
	  * A regular expression that finds delimiters from text
	  */
	lazy val anyDelimiterRegex = 
		(startingParenthesisRegex || endingParenthesisRegex || quotationRegex || spacedDelimiterRegex ||surroundedDashRegex || Regex.newLine).withinParenthesis + 
			Regex.newLine.anyTimes
}

