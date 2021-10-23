package utopia.citadel.database.access.single.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationDeletionCancellationFactory
import utopia.citadel.database.model.organization.OrganizationDeletionCancellationModel
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct OrganizationDeletionCancellations.
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait UniqueOrganizationDeletionCancellationAccess 
	extends SingleRowModelAccess[OrganizationDeletionCancellation] 
		with DistinctModelAccess[OrganizationDeletionCancellation, Option[OrganizationDeletionCancellation], Value] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the cancelled deletion. None if no instance (or value) was found.
	  */
	def deletionId(implicit connection: Connection) = pullColumn(model.deletionIdColumn).int
	
	/**
	  * Id of the user who cancelled the referenced organization deletion, 
		if still known. None if no instance (or value) was found.
	  */
	def creatorId(implicit connection: Connection) = pullColumn(model.creatorIdColumn).int
	
	/**
	  * Time when this OrganizationDeletionCancellation was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionCancellationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionCancellationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def creatorId_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	
	/**
	  * Updates the deletionId of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newDeletionId A new deletionId to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def deletionId_=(newDeletionId: Int)(implicit connection: Connection) = 
		putColumn(model.deletionIdColumn, newDeletionId)
}

