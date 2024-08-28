package utopia.logos.database.access.single.text.word.placement

import utopia.logos.database.factory.text.WordPlacementDbFactory
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual word placements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object DbWordPlacement extends SingleRowModelAccess[WordPlacement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Model which contains the primary database properties interacted with in this access point
	  */
	private def model = WordPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = WordPlacementDbFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted word placement
	  * @return An access point to that word placement
	  */
	def apply(id: Int) = DbSingleWordPlacement(id)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique word placements.
	  * @return An access point to the word placement that satisfies the specified condition
	  */
	private def distinct(condition: Condition) = UniqueWordPlacementAccess(condition)
}

