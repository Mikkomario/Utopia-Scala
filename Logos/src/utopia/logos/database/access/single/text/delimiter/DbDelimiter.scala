package utopia.logos.database.access.single.text.delimiter

import utopia.logos.database.factory.text.DelimiterDbFactory
import utopia.logos.database.storable.text.DelimiterDbModel
import utopia.logos.model.stored.text.Delimiter
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual delimiters
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbDelimiter extends SingleRowModelAccess[Delimiter] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	private def model = DelimiterDbModel
	
	
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
	private def distinct(condition: Condition) = UniqueDelimiterAccess(condition)
}

