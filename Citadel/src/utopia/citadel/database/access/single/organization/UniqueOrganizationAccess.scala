package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationFactory
import utopia.citadel.database.model.organization.OrganizationModel
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Organization
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct Organizations.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueOrganizationAccess 
	extends SingleRowModelAccess[Organization] 
		with DistinctModelAccess[Organization, Option[Organization], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who created this organization (if still known). None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	
	/**
	  * Time when this Organization was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted Organization instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any Organization instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted Organization instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any Organization instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
}

