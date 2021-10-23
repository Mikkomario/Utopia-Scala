package utopia.citadel.database.access.many.organization

import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple OrganizationDeletions at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbOrganizationDeletions extends ManyOrganizationDeletionsAccess with UnconditionalView
{
	// COMPUTED --------------------
	
	/**
	  * @return An access point to organization deletions including their cancellations
	  */
	def withCancellations = DbOrganizationDeletionsWithCancellations
	
	
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted OrganizationDeletions
	  * @return An access point to OrganizationDeletions with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbOrganizationDeletionsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbOrganizationDeletionsSubset(targetIds: Set[Int]) extends ManyOrganizationDeletionsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

