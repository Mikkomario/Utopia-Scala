package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.{DeletionCancelFactory, DeletionWithCancellationsFactory}
import utopia.citadel.database.model.organization.{DeletionCancelModel, DeletionModel}
import utopia.flow.time.Now
import utopia.metropolis.model.combined.organization.DeletionWithCancellations
import utopia.metropolis.model.partial.organization.DeletionCancelData
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator.Smaller
import utopia.vault.nosql.access.ManyModelAccess
import utopia.vault.sql.{Delete, Where}

import java.time.Instant

/**
  * Common trait for access points into organization deletions
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1.0
  */
trait OrganizationDeletionsAccess extends ManyModelAccess[DeletionWithCancellations]
{
	// COMPUTED	--------------------------
	
	/**
	  * @return The (primary) model class used by this access point
	  */
	protected def model = DeletionModel
	
	/**
	  * @return Factory used for reading organization deletion cancellations
	  */
	protected def cancellationFactory = DeletionCancelFactory
	
	/**
	  * @return An access point to pending deletions (those not cancelled)
	  */
	def pending = PendingDeletionsAccess
	
	/**
	  * @param connection Implicit DB Connection
	  * @return Organization deletions that have been cancelled
	  */
	def cancelled(implicit connection: Connection) =
		find(cancellationFactory.table.primaryColumn.get.isNotNull)
	
	
	// IMPLEMENTED	----------------------
	
	override def factory = DeletionWithCancellationsFactory
	
	
	// OTHER    --------------------------
	
	/**
	  * @param threshold A time threshold
	  * @param connection Implicit DB Connection
	  * @return Organization deletions (with cancellations) that were cancelled before the specified time threshold
	  */
	def cancelledBefore(threshold: Instant)(implicit connection: Connection) =
		find(cancellationFactory.createdBeforeCondition(threshold))
	
	/**
	  * Deletes organization deletions that have been cancelled before the specified time threshold
	  * @param threshold A time threshold
	  * @param connection Implicit DB connection
	  * @return The number of deleted deletions
	  */
	def deleteIfCancelledBefore(threshold: Instant)(implicit connection: Connection) =
		connection(Delete(target, table) +
			Where(mergeCondition(cancellationFactory.createdBeforeCondition(threshold)))).updatedRowCount
	
	
	// NESTED	--------------------------
	
	object PendingDeletionsAccess extends ManyModelAccess[DeletionWithCancellations]
	{
		// COMPUTED ----------------------
		
		/**
		  * @param connection Implicit DB Connection
		  * @return Pending (non-cancelled) deletions that have been scheduled to actualize
		  */
		def readyToActualize(implicit connection: Connection) =
			actualizingBefore(Now)
		
		
		// IMPLEMENTED	------------------
		
		override def factory = OrganizationDeletionsAccess.this.factory
		
		override def globalCondition = Some(OrganizationDeletionsAccess.this.mergeCondition(
			DeletionCancelModel.table.primaryColumn.get.isNull))
		
		override protected def defaultOrdering = None
		
		
		// OTHER	----------------------
		
		/**
		  * @param threshold A time threshold
		  * @param connection Implicit DB Connection
		  * @return Those of these pending deletions that are scheduled to actualize before the specified time threshold
		  */
		def actualizingBefore(threshold: Instant)(implicit connection: Connection) =
			find(model.withActualizationTime(threshold).toConditionWithOperator(Smaller))
		
		/**
		  * Cancels all pending deletions accessible from this acess point
		  * @param creatorId  Id of the user who cancels these deletions
		  * @param connection DB Connection (implicit)
		  * @return Affected deletions, along with the new cancellations
		  */
		def cancel(creatorId: Int)(implicit connection: Connection) =
		{
			// Inserts a new deletion cancel for all pending deletions
			val pendingDeletions = all
			pendingDeletions.map { deletion =>
				val cancellation = DeletionCancelModel.insert(DeletionCancelData(deletion.id, Some(creatorId)))
				DeletionWithCancellations(deletion, Vector(cancellation))
			}
		}
	}
}
