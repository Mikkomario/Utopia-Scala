package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.{DeletionFactory, DeletionWithCancellationsFactory}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.organization.Deletion
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIdModelAccess

/**
  * An access point to individual organization deletions
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.0
  */
object DbOrganizationDeletion extends SingleRowModelAccess[Deletion]
{
	// COMPUTED ----------------------------------
	
	private def withCancellationsFactory = DeletionWithCancellationsFactory
	
	
	// IMPLEMENTED  ------------------------------
	
	override def factory = DeletionFactory
	
	override def globalCondition = None
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param deletionId A deletion id
	  * @return An access point to that deletion's data
	  */
	def apply(deletionId: Int) = new DbSingleOrganizationDeletion(deletionId)
	
	
	// NESTED   ----------------------------------
	
	class DbSingleOrganizationDeletion(deletionId: Int)
		extends SingleIdModelAccess[Deletion](deletionId, DbOrganizationDeletion.factory)
	{
		/**
		  * Checks whether this deletion has been cancelled
		  * @param connection Implicit database connection
		  * @return Whether this deletion has been cancelled
		  */
		def isCancelled(implicit connection: Connection) =
			withCancellationsFactory.existsLinkWhere(condition)
		
		/**
		  * @param connection Implicit DB Connection
		  * @return Whether this deletion is still pending (hasn't been cancelled)
		  */
		def isPending(implicit connection: Connection) = withCancellationsFactory.isWithoutLinkWhere(condition)
	}
}
