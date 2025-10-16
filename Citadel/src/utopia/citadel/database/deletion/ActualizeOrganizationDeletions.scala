package utopia.citadel.database.deletion

import utopia.citadel.database.access.many.organization.{DbOrganizationDeletions, DbOrganizations}
import utopia.flow.time.{Duration, Now}
import utopia.vault.database.Connection

/**
  * An object for actualizing scheduled, non-cancelled organization deletions
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.0
  */
object ActualizeOrganizationDeletions
{
	/**
	  * Actualizes pending deletions and possibly deletes some cancelled deletions
	  * @param cancelledDeletionHistoryDuration Duration how long to keep cancelled deletions in the database
	  *                                         (default = infinite = never delete deletions)
	  * @param connection Implicit DB Connection
	  */
	def apply(cancelledDeletionHistoryDuration: Duration = Duration.infinite)(implicit connection: Connection) = {
		// Checks if there are some cancelled deletions that need to be deleted
		cancelledDeletionHistoryDuration.ifFinite.foreach { historyDuration =>
			DbOrganizationDeletions.withCancellations.cancelledBefore(Now - historyDuration).delete()
		}
		
		// Checks if there are pending deletions
		val targetOrganizationIds = DbOrganizationDeletions.withCancellations.notCancelled.readyToActualize
			.organizationIds.toSet
		// Actualizes the deletions
		if (targetOrganizationIds.nonEmpty)
			DbOrganizations(targetOrganizationIds).delete()
	}
}
