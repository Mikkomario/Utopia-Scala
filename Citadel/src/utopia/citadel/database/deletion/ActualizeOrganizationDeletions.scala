package utopia.citadel.database.deletion

import utopia.citadel.database.access.many.organization.{DbOrganizationDeletions, DbOrganizations}
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.Now
import utopia.vault.database.Connection

import scala.concurrent.duration.Duration

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
	  * @return Number of deleted organizations
	  */
	def apply(cancelledDeletionHistoryDuration: Duration = Duration.Inf)(implicit connection: Connection) =
	{
		// Checks if there are some cancelled deletions that need to be deleted
		cancelledDeletionHistoryDuration.finite.foreach { historyDuration =>
			DbOrganizationDeletions.deleteIfCancelledBefore(Now - historyDuration)
		}
		
		// Checks if there are pending deletions
		val ready = DbOrganizationDeletions.pending.readyToActualize
		// Actualizes the deletions
		if (ready.nonEmpty)
			DbOrganizations.withIds(ready.map { _.organizationId }.toSet).delete()
		else
			0
	}
}
