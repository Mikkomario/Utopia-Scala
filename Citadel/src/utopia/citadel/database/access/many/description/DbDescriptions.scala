package utopia.citadel.database.access.many.description

import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * The root access point when targeting multiple Descriptions at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbDescriptions extends ManyDescriptionsAccess with NonDeprecatedView[Description]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Descriptions
	  * @return An access point to Descriptions with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbDescriptionsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbDescriptionsSubset(targetIds: Set[Int]) extends ManyDescriptionsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

