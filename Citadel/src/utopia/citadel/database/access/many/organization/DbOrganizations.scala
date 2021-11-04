package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.many.description.ManyDescribedAccessByIds
import utopia.metropolis.model.combined.organization.DescribedOrganization
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple Organizations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbOrganizations extends ManyOrganizationsAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted Organizations
	  * @return An access point to Organizations with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbOrganizationsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbOrganizationsSubset(override val ids: Set[Int]) 
		extends ManyOrganizationsAccess with ManyDescribedAccessByIds[Organization, DescribedOrganization]
	{
		/**
		  * @return An access point to deletions concerning these organizations
		  */
		def deletions = DbOrganizationDeletions.forAnyOfOrganizations(ids)
	}
}

