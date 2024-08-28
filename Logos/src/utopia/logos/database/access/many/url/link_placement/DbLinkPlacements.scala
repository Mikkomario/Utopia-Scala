package utopia.logos.database.access.many.url.link_placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple link placements at a time
  * @author Mikko Hilpinen
  * @since 16.10.2023, v0.1
  */
@deprecated("Replaced with a new version", "v0.3")
object DbLinkPlacements extends ManyLinkPlacementsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted link placements
	  * @return An access point to link placements with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbLinkPlacementsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbLinkPlacementsSubset(targetIds: Set[Int]) extends ManyLinkPlacementsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

