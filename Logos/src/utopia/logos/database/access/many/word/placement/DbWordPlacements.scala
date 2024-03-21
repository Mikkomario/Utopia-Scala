package utopia.logos.database.access.many.word.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple word placements at a time
  * @author Mikko Hilpinen
  * @since 12.10.2023, v0.1
  */
object DbWordPlacements extends ManyWordPlacementsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted word placements
	  * @return An access point to word placements with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbWordPlacementsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbWordPlacementsSubset(targetIds: Set[Int]) extends ManyWordPlacementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

