package utopia.exodus.database.access.many

import utopia.exodus.database.model.organization.{DeletionModel, MemberRoleModel, MembershipModel, OrganizationModel}
import utopia.exodus.model.enumeration.StandardDescriptionRoleId
import utopia.exodus.model.enumeration.StandardUserRole.Owner
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.organization.MembershipData
import utopia.vault.database.Connection
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple organizations at a time
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
object DbOrganizations
{
	// COMPUTED	----------------------

	private def factory = OrganizationModel


	// OTHER	-----------------------

	/**
	  * @param ids Organization ids
	  * @return An access point to organizations with those ids
	  */
	def withIds(ids: Set[Int]) = new OrganizationsWithIds(ids)

	/**
	  * Inserts a new organization to the database
	  * @param founderId Id of the user who created the organization
	  * @param connection DB Connection (implicit)
	  * @return Id of the newly inserted organization
	  */
	def insert(organizationName: String, languageId: Int, founderId: Int)(implicit connection: Connection) =
	{
		// Inserts a new organization
		val organizationId = factory.insert(founderId)
		// Adds the user to the organization (as owner)
		val membership = MembershipModel.insert(MembershipData(organizationId, founderId, Some(founderId)))
		MemberRoleModel.insert(membership.id, Owner.id, founderId)
		// Inserts a name for that organization
		DbDescriptions.ofOrganizationWithId(organizationId).update(StandardDescriptionRoleId.name, languageId,
			founderId, organizationName)
		// Returns organization id
		organizationId
	}


	// NESTED	------------------------

	class OrganizationsWithIds(organizationIds: Set[Int])
	{
		// COMPUTED	--------------------

		/**
		  * @return An access point to deletions concerning these organizations
		  */
		def deletions = Deletions


		// NESTED	--------------------

		object Deletions extends OrganizationDeletionsAccess
		{
			// IMPLEMENTED	------------

			override def globalCondition = Some(DeletionModel.organizationIdColumn.in(organizationIds))
			
			override protected def defaultOrdering = None
		}
	}
}
