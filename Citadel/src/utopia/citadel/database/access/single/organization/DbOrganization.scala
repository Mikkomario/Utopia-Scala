package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.OrganizationFactory
import utopia.citadel.database.model.organization.OrganizationModel
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Organizations
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbOrganization extends SingleRowModelAccess[Organization] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Organization instance
	  * @return An access point to that Organization
	  */
	def apply(id: Int) = DbSingleOrganization(id)
}

