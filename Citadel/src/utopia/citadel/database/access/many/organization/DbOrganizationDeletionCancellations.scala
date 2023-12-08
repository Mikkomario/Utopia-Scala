package utopia.citadel.database.access.many.organization

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple OrganizationDeletionCancellations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbOrganizationDeletionCancellations 
	extends ManyOrganizationDeletionCancellationsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted OrganizationDeletionCancellations
	  * @return An access point to OrganizationDeletionCancellations with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbOrganizationDeletionCancellationsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbOrganizationDeletionCancellationsSubset(targetIds: Set[Int]) 
		extends ManyOrganizationDeletionCancellationsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def accessCondition = Some(index in targetIds)
	}
}

