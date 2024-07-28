package utopia.logos.database.access.single.word.placement

import utopia.logos.database.factory.word.WordPlacementDbFactory
import utopia.logos.database.storable.word.WordPlacementModel
import utopia.logos.model.stored.word.WordPlacement
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

/**
  * Used for accessing individual word placements
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
object DbWordPlacement extends SingleRowModelAccess[WordPlacement] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = WordPlacementModel
	
	
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
	protected def filterDistinct(condition: Condition) = UniqueWordPlacementAccess(mergeCondition(condition))
}

