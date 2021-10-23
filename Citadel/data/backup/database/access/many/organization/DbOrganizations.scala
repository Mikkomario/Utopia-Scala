package utopia.citadel.database.access.many.organization

import utopia.citadel.database.access.single.organization.DbOrganization
import utopia.citadel.database.model.organization.{DeletionModel, OrganizationModel}
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.sql.{Delete, Where}
import utopia.vault.sql.SqlExtensions._

/**
  * Used for accessing multiple organizations at a time
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1.0
  */
object DbOrganizations
{
	// COMPUTED	----------------------
	
	private def factory = OrganizationModel
	
	private def table = factory.table
	
	
	// OTHER	-----------------------
	
	/**
	  * @param ids Organization ids
	  * @return An access point to organizations with those ids
	  */
	def withIds(ids: Set[Int]) = new OrganizationsWithIds(ids)
	
	/**
	  * Inserts a new organization to the database
	  * @param founderId  Id of the user who created the organization
	  * @param connection DB Connection (implicit)
	  * @return Id of the newly inserted organization
	  */
	@deprecated("Please call this method from DbOrganization instead", "v1.3")
	def insert(organizationName: String, languageId: Int, founderId: Int)(implicit connection: Connection) =
		DbOrganization.insert(organizationName, languageId, founderId)
	
	
	// NESTED	------------------------
	
	class OrganizationsWithIds(organizationIds: Set[Int])
	{
		// COMPUTED	--------------------
		
		private def condition = DeletionModel.organizationIdColumn.in(organizationIds)
		
		/**
		  * @return An access point to deletions concerning these organizations
		  */
		def deletions = Deletions
		
		
		// OTHER    --------------------
		
		/**
		  * Deletes all of the organizations accessible from this access point
		  * @param connection Implicit DB Connection
		  * @return The number of deleted organizations
		  */
		def delete()(implicit connection: Connection) =
			connection(Delete(table) + Where(condition)).updatedRowCount
		
		
		// NESTED	--------------------
		
		object Deletions extends OrganizationDeletionsAccess
		{
			// IMPLEMENTED	------------
			
			override def globalCondition = Some(OrganizationsWithIds.this.condition)
			
			override protected def defaultOrdering = None
		}
	}
}
