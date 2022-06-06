package utopia.vault.database

import utopia.flow.async.LoopingProcess
import utopia.flow.util.CollectionExtensions._
import utopia.vault.model.immutable.{Reference, Table}
import utopia.vault.sql.{Condition, Delete, Join, JoinType, SqlTarget, Where}

import java.time.LocalTime
import scala.concurrent.ExecutionContext

object ClearUnreferencedData
{
	/**
	  * Clears unreferenced data from the targeted tables.
	  * @param tables Target tables
	  * @param connection Implicit DB Connection
	  * @return Number of deleted items in total
	  */
	def onceFrom(tables: Set[Table])(implicit connection: Connection) =
		new ClearUnreferencedData(tables.map { _ -> Set() })()
	
	def onceFrom(table: Table, ignoringReferencesFrom: Set[Table] = Set())
	            (implicit connection: Connection) =
		new ClearUnreferencedData(Set(table -> ignoringReferencesFrom))()
	
	def onceFrom(first: Table, second: Table, more: Table*)(implicit connection: Connection): Int =
		onceFrom(Set(first, second) ++ more)
	
	def loopDailyFrom(tables: Set[Table], at: LocalTime = LocalTime.MIDNIGHT,
	                  onError: Throwable => Unit = _.printStackTrace())
	                 (implicit exc: ExecutionContext, connectionPool: ConnectionPool) =
	{
		val deleter = new ClearUnreferencedData(tables.map { _ -> Set() })
		LoopingProcess.daily(at) { _ => connectionPool.tryWith { implicit c => deleter()  }.failure.foreach(onError) }
	}
}

/**
  * This class clears unreferenced data from targeted tables on demand
  * @author Mikko Hilpinen
  * @since 28.6.2021, v1.8
  * @param targets The tables targeted by this deletion, along with a set of tables that are NOT considered as valid
  *                reference origins. Please note that if you include any tables with no other table referencing them,
  *                all rows will be deleted on each iteration
  */
class ClearUnreferencedData(targets: Set[(Table, Set[Table])])
{
	// ATTRIBUTES   --------------------------
	
	// Orders the deletions so that the possibly referencing tables are cleared first
	private lazy val deletionTargets = orderTargets(targets.toVector.map { case (table, ignored) =>
		// Searches for the references towards that table, but ignores specified tables
		DeletionTarget(table, References.to(table).filterNot { ref => ignored.contains(ref.from.table) }.toVector)
	})
	
	
	// OTHER    ------------------------------
	
	/**
	  * Performs the deletion once, deleting all unreferenced items from the targeted tables
	  * @param connection Implicit database connection
	  * @return The number of deleted items
	  */
	def apply()(implicit connection: Connection) = deletionTargets.map { target =>
		connection(target.toSqlStatement).updatedRowCount
	}.sum
	
	private def orderTargets(targets: Vector[DeletionTarget]): Vector[DeletionTarget] =
	{
		if (targets.size < 2)
			targets
		else
		{
			// Finds the targets which contain references to other tables
			val targetsByReferenceCount = targets.groupBy { target =>
				targets.map { _.references.count { _.from.table == target.table } }.sum
			}
			val maxReferenceCount = targetsByReferenceCount.keys.max
			
			// If the results were split, attempts to order the groups with references
			if (maxReferenceCount == 0 || targetsByReferenceCount.size == 1)
				targets
			else
			{
				val zeroReferenceTargets = targetsByReferenceCount.getOrElse(0, Vector())
				val orderedReferencingTargets = (targetsByReferenceCount - 0)
					.toVector.sortBy { -_._1 }.map { _._2 }.flatMap(orderTargets)
				orderedReferencingTargets ++ zeroReferenceTargets
			}
		}
	}
	
	
	// NESTED   ------------------------------
	
	case class DeletionTarget(table: Table, references: Vector[Reference])
	{
		def toSqlStatement = {
			// Performs all reference joins
			val target = references.foldLeft(table: SqlTarget) { (target, reference) =>
				target + Join(reference.to.column, reference.from, JoinType.Left)
			}
			// Requires all the joins to fail / not connect
			val conditions = references.map { ref => ref.from.table.primaryColumn.getOrElse(ref.from.column).isNull }
			// Forms the deletion statement
			Delete(target, table) + Where(Condition.and(conditions))
		}
	}
}
