package utopia.citadel.database.deletion

import utopia.citadel.database.access.many.organization.{DbOrganizationDeletions, DbOrganizations}
import utopia.citadel.database.access.single.organization.{DbOrganization, DbOrganizationDeletion}
import utopia.flow.async.DailyTask
import utopia.flow.time.{Now, WaitUtils}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.CollectionExtensions._
import utopia.metropolis.model.stored.organization.Deletion
import utopia.vault.database.ConnectionPool

import java.time.LocalTime
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.Duration

/**
  * A daily loop for actualizing pending organization deletions
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.0
  */
class OrganizationDeletionActualizer(cancelledDeletionHistoryDuration: Duration,
                                     override val triggerTime: LocalTime = LocalTime.MIDNIGHT,
                                     onError: Throwable => Unit = _.printStackTrace())
                                    (implicit exc: ExecutionContext, connectionPool: ConnectionPool)
	extends DailyTask
{
	override def runOnce() =
	{
		connectionPool.tryWith { implicit connection =>
			// Checks if there are some cancelled deletions that need to be deleted
			cancelledDeletionHistoryDuration.finite.foreach { historyDuration =>
				DbOrganizationDeletions.deleteIfCancelledBefore(Now - historyDuration)
			}
			
			// Checks if there are pending deletions in the past or during the next 24 hours
			val (pending, ready) = DbOrganizationDeletions.pending.actualizingBefore(Now + 24.hours)
				.divideBy { _.actualizationTime.isInPast }
			
			// Actualizes some of the deletions
			if (ready.nonEmpty)
				DbOrganizations.withIds(ready.map { _.organizationId }.toSet).delete()
			
			// If there are pending deletions, schedules actualization for those (async)
			if (pending.nonEmpty)
				scheduleActualizationFor(pending.map { _.deletion }.sortBy { _.actualizationTime }, onError)
			
		}.failure.foreach(onError)
	}
	
	// Expects a non-empty and ordered deletions parameter
	// TODO: Handle cases where jvm closes in the middle of scheduling (should stop?)
	private def scheduleActualizationFor(deletions: Vector[Deletion], onError: Throwable => Unit)
	                                    (implicit exc: ExecutionContext, connectionPool: ConnectionPool): Unit =
	{
		Future {
			deletions.foreach { deletion =>
				// Waits until the deletion should be actualized
				WaitUtils.waitUntil(deletion.actualizationTime)
				
				connectionPool.tryWith { implicit connection =>
					// Checks whether the deletion was cancelled during the wait
					if (DbOrganizationDeletion(deletion.id).isPending)
					{
						// If not, deletes the organization
						DbOrganization(deletion.organizationId).delete()
					}
				}.failure.foreach(onError)
			}
		}
	}
}
