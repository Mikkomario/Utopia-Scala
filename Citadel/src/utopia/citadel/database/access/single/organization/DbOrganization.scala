package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.OrganizationFactory
import utopia.citadel.database.model.description.CitadelDescriptionLinkModel
import utopia.citadel.database.model.organization.OrganizationModel
import utopia.citadel.model.enumeration.CitadelDescriptionRole.Name
import utopia.citadel.model.enumeration.StandardUserRole.Owner
import utopia.metropolis.model.combined.organization.DescribedOrganization
import utopia.metropolis.model.partial.description.DescriptionData
import utopia.metropolis.model.partial.organization.OrganizationData
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.database.Connection
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
	
	/**
	  * Starts / creates a new organization
	  * @param organizationName Name of this organization
	  * @param languageId Id of the language in which this organization name is given
	  * @param founderId  Id of the user who created the organization
	  * @param connection DB Connection (implicit)
	  * @return Newly created organization + newly started membership in that organization
	  */
	def found(organizationName: String, languageId: Int, founderId: Int)(implicit connection: Connection) =
	{
		// Inserts a new organization
		val organization = model.insert(OrganizationData(Some(founderId)))
		// Inserts a name for that organization
		val nameDescription = CitadelDescriptionLinkModel.organization
			.insert(organization.id, DescriptionData(Name.id, languageId, organizationName, Some(founderId)))
		// Adds the user to the organization (as owner)
		val membership = DbMembership.start(organization.id, founderId, Owner.id, Some(founderId))
		
		// Returns the organization and the membership
		DescribedOrganization(organization, Set(nameDescription)) -> membership
	}
}

