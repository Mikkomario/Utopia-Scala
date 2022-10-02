package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationDeletionFactory
import utopia.citadel.database.model.organization.OrganizationDeletionModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct OrganizationDeletions.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueOrganizationDeletionAccess 
	extends SingleRowModelAccess[OrganizationDeletion] 
		with DistinctModelAccess[OrganizationDeletion, Option[OrganizationDeletion], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the organization whose deletion was requested. None if no instance (or value) was found.
	  */
	def organizationId(implicit connection: Connection) = pullColumn(model.organizationIdColumn).int
	/**
	  * Time when this deletion is/was scheduled to actualize. None if no instance (or value) was found.
	  */
	def actualization(implicit connection: Connection) = pullColumn(model.actualizationColumn).instant
	/**
	  * Id of the user who requested organization deletion. None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	/**
	  * Time when this deletion was requested. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the actualization of the targeted OrganizationDeletion instance(s)
	  * @param newActualization A new actualization to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def actualization_=(newActualization: Instant)(implicit connection: Connection) = 
		putColumn(model.actualizationColumn, newActualization)
	/**
	  * Updates the created of the targeted OrganizationDeletion instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	/**
	  * Updates the creatorId of the targeted OrganizationDeletion instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	/**
	  * Updates the organizationId of the targeted OrganizationDeletion instance(s)
	  * @param newOrganizationId A new organizationId to assign
	  * @return Whether any OrganizationDeletion instance was affected
	  */
	def organizationId_=(newOrganizationId: Int)(implicit connection: Connection) = 
		putColumn(model.organizationIdColumn, newOrganizationId)
}

