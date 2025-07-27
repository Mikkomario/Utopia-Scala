package utopia.logos.model.stored.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.flow.parse.string.Regex
import utopia.logos.database.access.single.text.delimiter.DbSingleDelimiter
import utopia.logos.model.factory.text.DelimiterFactoryWrapper
import utopia.logos.model.partial.text.DelimiterData
import utopia.vault.store.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

object Delimiter extends StoredFromModelFactory[DelimiterData, Delimiter]
{
	// COMPUTED ------------------------
	
	/**
	 * @return Access to delimiter-related regular expressions
	 */
	def regex = Regexes
	
	@deprecated("Replaced with regex.any", "v0.6")
	def anyDelimiterRegex = regex.any
			
	
	// IMPLEMENTED	--------------------
	
	override def dataFactory = DelimiterData
	
	override protected def complete(model: AnyModel, data: DelimiterData) = 
		model("id").tryInt.map { apply(_, data) }
		
	
	// NESTED   ------------------------
	
	object Regexes
	{
		private lazy val dash = Regex.escape('-')
		private lazy val quotation = Regex("\\\"")
		
		/**
		 * Delimiters which require a space after them
		 */
		private lazy val spaced =
			(Regex.anyOf(",.!?:;)]").oneOrMoreTimes +
				(Regex.whiteSpace || Regex.endOfString || Regex.newLine).withinParentheses).withinParentheses
		
		private lazy val surroundedDash = (Regex.whiteSpace + dash + Regex.whiteSpace).withinParentheses
		
		/**
		 * A regular expression that finds delimiters from text
		 */
		lazy val any = Regex.whiteSpace.noneOrOnce +
			(Regex.anyOf("()<>{}[]") || quotation || spaced || surroundedDash || Regex.newLine).withinParentheses +
			Regex.newLine.anyTimes
	}
}

/**
  * Represents a delimiter that has already been stored in the database
  * @param id id of this delimiter in the database
  * @param data Wrapped delimiter data
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class Delimiter(id: Int, data: DelimiterData) 
	extends StoredModelConvertible[DelimiterData] with FromIdFactory[Int, Delimiter] 
		with DelimiterFactoryWrapper[DelimiterData, Delimiter]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this delimiter in the database
	  */
	def access = DbSingleDelimiter(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: DelimiterData) = copy(data = data)
}

