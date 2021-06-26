package utopia.citadel.database.access.many.organization

import utopia.citadel.database.factory.organization.DeletionFactory
import utopia.citadel.database.model.organization.DeletionCancelModel
import utopia.metropolis.model.combined.organization.DeletionWithCancellations
import utopia.metropolis.model.partial.organization.DeletionCancelData
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess

/**
  * Common trait for access points into organization deletions
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
trait OrganizationDeletionsAccess extends ManyModelAccess[DeletionWithCancellations]
{
	// IMPLEMENTED	----------------------
	
	override def factory = DeletionFactory
	
	
	// COMPUTED	--------------------------
	
	/**
	  * @return An access point to pending deletions (those not cancelled)
	  */
	def pending = Pending
	
	
	// NESTED	--------------------------
	
	object Pending extends ManyModelAccess[DeletionWithCancellations]
	{
		// IMPLEMENTED	------------------
		
		override def factory = OrganizationDeletionsAccess.this.factory
		
		override def globalCondition = Some(OrganizationDeletionsAccess.this.mergeCondition(
			DeletionCancelModel.table.primaryColumn.get.isNull))
		
		override protected def defaultOrdering = None
		
		
		// OTHER	----------------------
		
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
