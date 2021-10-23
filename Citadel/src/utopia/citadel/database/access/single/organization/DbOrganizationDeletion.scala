package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.OrganizationDeletionFactory
import utopia.citadel.database.model.organization.OrganizationDeletionModel
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual OrganizationDeletions
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbOrganizationDeletion 
	extends SingleRowModelAccess[OrganizationDeletion] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted OrganizationDeletion instance
	  * @return An access point to that OrganizationDeletion
	  */
	def apply(id: Int) = DbSingleOrganizationDeletion(id)
}

