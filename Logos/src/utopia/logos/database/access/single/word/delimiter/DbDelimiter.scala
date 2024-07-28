package utopia.logos.database.access.single.word.delimiter

import utopia.logos.database.factory.word.DelimiterDbFactory
import utopia.logos.database.storable.word.DelimiterModel
import utopia.logos.model.stored.word.Delimiter
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual delimiters
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbDelimiter extends SingleRowModelAccess[Delimiter] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DelimiterModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DelimiterDbFactory
	
	
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

