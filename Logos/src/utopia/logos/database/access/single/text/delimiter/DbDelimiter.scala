package utopia.logos.database.access.single.text.delimiter

import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition
import utopia.logos.database.factory.text.DelimiterFactory
import utopia.logos.database.model.text.DelimiterModel
import utopia.logos.model.stored.text.Delimiter

/**
  * Used for accessing individual delimiters
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object DbDelimiter extends SingleRowModelAccess[Delimiter] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DelimiterModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted delimiter
	  * @return An access point to that delimiter
	  */
	def apply(id: Int) = DbSingleDelimiter(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique delimiters.
	  * @return An access point to the delimiter that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueDelimiterAccess(mergeCondition(condition))
}

