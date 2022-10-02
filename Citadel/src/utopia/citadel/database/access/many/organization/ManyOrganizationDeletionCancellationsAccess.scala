package utopia.citadel.database.access.many.organization

import java.time.Instant
import utopia.citadel.database.factory.organization.OrganizationDeletionCancellationFactory
import utopia.citadel.database.model.organization.OrganizationDeletionCancellationModel
import utopia.flow.generic.casting.ValueConversions._
import utopia.metropolis.model.stored.organization.OrganizationDeletionCancellation
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{FilterableView, SubView}
import utopia.vault.sql.Condition

object ManyOrganizationDeletionCancellationsAccess
{
	// NESTED	--------------------
	
	private class ManyOrganizationDeletionCancellationsSubView(override val parent: ManyRowModelAccess[OrganizationDeletionCancellation], 
		override val filterCondition: Condition) 
		extends ManyOrganizationDeletionCancellationsAccess with SubView
}

/**
  * A common trait for access points which target multiple OrganizationDeletionCancellations at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyOrganizationDeletionCancellationsAccess 
	extends ManyRowModelAccess[OrganizationDeletionCancellation] with Indexed
		with FilterableView[ManyOrganizationDeletionCancellationsAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * deletionIds of the accessible OrganizationDeletionCancellations
	  */
	def deletionIds(implicit connection: Connection) = 
		pullColumn(model.deletionIdColumn).flatMap { value => value.int }
	
	/**
	  * creatorIds of the accessible OrganizationDeletionCancellations
	  */
	def creatorIds(implicit connection: Connection) = 
		pullColumn(model.creatorIdColumn).flatMap { value => value.int }
	
	/**
	  * creationTimes of the accessible OrganizationDeletionCancellations
	  */
	def creationTimes(implicit connection: Connection) = 
		pullColumn(model.createdColumn).flatMap { value => value.instant }
	
	def ids(implicit connection: Connection) = pullColumn(index).flatMap { id => id.int }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = OrganizationDeletionCancellationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = OrganizationDeletionCancellationFactory
	
	override def filter(additionalCondition: Condition): ManyOrganizationDeletionCancellationsAccess = 
		new ManyOrganizationDeletionCancellationsAccess.ManyOrganizationDeletionCancellationsSubView(this, 
			additionalCondition)
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the creatorId of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newCreatorId A new creatorId to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def creatorIds_=(newCreatorId: Int)(implicit connection: Connection) = 
		putColumn(model.creatorIdColumn, newCreatorId)
	
	/**
	  * Updates the deletionId of the targeted OrganizationDeletionCancellation instance(s)
	  * @param newDeletionId A new deletionId to assign
	  * @return Whether any OrganizationDeletionCancellation instance was affected
	  */
	def deletionIds_=(newDeletionId: Int)(implicit connection: Connection) = 
		putColumn(model.deletionIdColumn, newDeletionId)
}

