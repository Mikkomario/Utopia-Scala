package utopia.citadel.database.access.single.organization

import utopia.citadel.database.factory.organization.OrganizationDeletionWithCancellationsFactory
import utopia.metropolis.model.stored.organization.OrganizationDeletion
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual OrganizationDeletions, based on their id
  * @since 2021-10-23
  */
case class DbSingleOrganizationDeletion(id: Int) 
	extends UniqueOrganizationDeletionAccess with SingleIntIdModelAccess[OrganizationDeletion]
{
	private def withCancellationsFactory = OrganizationDeletionWithCancellationsFactory
	
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
	def nonCancelled(implicit connection: Connection) = withCancellationsFactory.isWithoutLinkWhere(condition)
}
