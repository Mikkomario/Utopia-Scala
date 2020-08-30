package utopia.exodus.database.model.organization

import utopia.exodus.database.Tables
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable

object OrganizationModel
{
	// COMPUTED	------------------------------
	
	/**
	  * @return The table used by this factory
	  */
	def table = Tables.organization
	
	
	// OTHER	------------------------------
	
	/**
	  * @param organizationId Id of the organization
	  * @return A model with only id set
	  */
	def withId(organizationId: Int) = apply(Some(organizationId))
	
	/**
	  * Inserts a new organization to the DB
	  * @param founderId Id of the user who created this organization
	  * @param connection DB Connection (implicit)
	  * @return A new organization
	  */
	def insert(founderId: Int)(implicit connection: Connection) = apply(creatorId = Some(founderId)).insert().getInt
}

/**
  * Used for interacting with organizations in DB
  * @author Mikko Hilpinen
  * @since 4.5.2020, v1
  */
case class OrganizationModel(id: Option[Int] = None, creatorId: Option[Int] = None) extends Storable
{
	override def table = OrganizationModel.table
	
	override def valueProperties = Vector("id" -> id, "creatorId" -> creatorId)
}
