package utopia.logos.model.stored.text

import utopia.flow.parse.string.Regex
import utopia.logos.database.access.single.text.delimiter.DbSingleDelimiter
import utopia.logos.model.partial.text.DelimiterData
import utopia.vault.model.template.StoredModelConvertible

object Delimiter
{
	// ATTRIBUTES   -----------------
	
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
		((commaRegex || periodRegex || exclamationRegex || questionRegex || colonRegex || endingParenthesisRegex)
			.withinParenthesis.oneOrMoreTimes +
			(Regex.whiteSpace || Regex.endOfString || Regex.newLine).withinParenthesis).withinParenthesis
	private lazy val surroundedDashRegex = (Regex.whiteSpace + dashRegex + Regex.whiteSpace).withinParenthesis
	
	/**
	 * A regular expression that finds delimiters from text
	 */
	lazy val anyDelimiterRegex =
		(startingParenthesisRegex || endingParenthesisRegex || quotationRegex || spacedDelimiterRegex ||
			surroundedDashRegex || Regex.newLine)
			.withinParenthesis + Regex.newLine.anyTimes
}

/**
  * Represents a delimiter that has already been stored in the database
  * @param id id of this delimiter in the database
  * @param data Wrapped delimiter data
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class Delimiter(id: Int, data: DelimiterData) extends StoredModelConvertible[DelimiterData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this delimiter in the database
	  */
	def access = DbSingleDelimiter(id)
	
	
	// IMPLEMENTED  ----------------
	
	override def toString = data.text
}

