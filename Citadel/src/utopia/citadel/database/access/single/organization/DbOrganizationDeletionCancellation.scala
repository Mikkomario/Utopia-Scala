package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.OrganizationDeletionCancellationFactory
import utopia.citadel.database.model.organization.OrganizationDeletionCancellationModel
import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual OrganizationDeletionCancellations
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbOrganizationDeletionCancellation 
	extends SingleRowModelAccess[OrganizationDeletionCancellation] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionCancellationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionCancellationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted OrganizationDeletionCancellation instance
	  * @return An access point to that OrganizationDeletionCancellation
	  */
	def apply(id: Int) = DbSingleOrganizationDeletionCancellation(id)
}

