package utopia.vault.database

import utopia.flow.async.process.LoopingProcess

import java.time.{Instant, LocalTime}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.sql.SqlExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.util.logging.Logger
import utopia.vault.model.error.NoReferenceFoundException
import utopia.vault.model.immutable.{DataDeletionRule, Reference, Table}
import utopia.vault.sql.{Condition, Delete, Join, SqlTarget, Where}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

object ClearOldData
{
	/**
	  * Clears old data once (immediately)
	  * @param rules Data deletion rules
	  * @param connection Implicit DB Connection
	  */
	def once(rules: Iterable[DataDeletionRule])(implicit connection: Connection) =
		new ClearOldData(rules)()
	/**
	  * Clears old data once (immediately)
	  * @param rule Data deletion rule
	  * @param connection Implicit DB Connection
	  */
	def once(rule: DataDeletionRule)(implicit connection: Connection): Unit = once(Vector(rule))
	/**
	  * Clears old data once (immediately)
	  * @param first The first deletion rule to apply
	  * @param second The second deletion rule to apply
	  * @param more More deletion rules to apply
	  * @param connection Implicit DB Connection
	  */
	def once(first: DataDeletionRule, second: DataDeletionRule, more: DataDeletionRule*)
	        (implicit connection: Connection): Unit = once(Vector(first, second) ++ more)
	
	/**
	  * Constructs a daily task / loop for deleting old data
	  * @param rules Deletion rules to use
	  * @param at The time when the data deletion is performed each day (local) (default = at midnight)
	  * @param exc Implicit execution context (for connection management)
	  * @param connectionPool Connection pool to use (implicit) for a connection for each daily operation
	  * @param logger A logger implementation to handle thrown errors with
	  * @return A new task / loop (not yet active or looping)
	  */
	def dailyLoop(rules: Iterable[DataDeletionRule], at: LocalTime = LocalTime.MIDNIGHT)
	             (implicit exc: ExecutionContext, connectionPool: ConnectionPool, logger: Logger) =
	{
		val clearer = new ClearOldData(rules)
		LoopingProcess.daily(at) { _ =>
			connectionPool.tryWith { implicit connection => clearer() }
				.failure.foreach { logger(_, "ClearOldData call failed") }
		}
	}
}

/**
 * Used for clearing deprecated data from the database
 * @author Mikko Hilpinen
 * @since 2.4.2020, v1.5
 */
class ClearOldData(rules: Iterable[DataDeletionRule])
{
	// ATTRIBUTES	---------------------------
	
	// Forms the actual deletion rules based on those provided + existing table references
	private val finalRules =
	{
		val nonEmptyRules =  rules.filter { _.nonEmpty }
		nonEmptyRules.map { rule =>
			// Checks if there exist any rules for tables referencing the table in question
			val tree = References.referenceTree(rule.targetTable)
			val restrictingChildren = nonEmptyRules.flatMap { childRule =>
				tree.filterWithPaths { _.nav == childRule.targetTable }.map { childPath =>
					// Converts the table path to a reference path
					// Throws possible errors here (those would result from logic / programming error)
					referencePathFrom(rule.targetTable, childPath.map { _.nav }).get
				}
			}.toVector
			// Creates the deletion rule for the primary table
			TableDeletionRule(rule.targetTable, rule.timePropertyName, rule.standardLiveDuration,
				rule.conditionalLiveDurations, restrictingChildren)
		}.toVector
	}
	
	
	// OTHER	------------------------------
	
	/**
	  * Performs the deletion operation
	  * @param connection Database connection used for the deletions
	  */
	def apply()(implicit connection: Connection) = deleteIteration(Now, finalRules, Set())
	
	@scala.annotation.tailrec
	private def deleteIteration(deletionTime: Instant, remainingRules: Vector[TableDeletionRule],
								handledTables: Set[Table])(implicit connection: Connection): Unit =
	{
		// During this iteration, can only handle tables that don't refer to unhandled tables
		val (nextIterationTargets, thisIterationTargets) = remainingRules.divideBy { rule =>
			rule.restrictiveChildTables.forall(handledTables.contains)
		}
		
		thisIterationTargets.foreach { rule =>
			// Checks the general deletion rule first
			rule.baseLiveDuration.finite.foreach { baseDuration =>
				val baseDurationCondition = rule.timeColumn < (deletionTime - baseDuration)
				performDeleteOn(rule, baseDurationCondition)
			}
			// Then checks individual conditions
			rule.conditionalPeriods.toVector.sortBy { _._2 }.foreach { case (condition, maxDuration) =>
				val durationCondition = rule.timeColumn < (deletionTime - maxDuration)
				performDeleteOn(rule, durationCondition && condition)
			}
		}
		
		// Performs the next iteration, if necessary
		// Will not continue if there exist only looping references
		if (nextIterationTargets.nonEmpty && thisIterationTargets.nonEmpty)
			deleteIteration(deletionTime, nextIterationTargets, handledTables ++ thisIterationTargets.map { _.table })
	}
	
	private def performDeleteOn(rule: TableDeletionRule, baseDeletionCondition: Condition)
	                           (implicit connection: Connection) =
	{
		// Checks whether child status should be checked as well
		val restrictions = rule.restrictiveChildPaths
		if (restrictions.isEmpty)
		{
			// If no restricting tables exist, simply deletes old data
			connection(Delete(rule.table) + Where(baseDeletionCondition))
		}
		else
		{
			// If there were restrictions, performs a join and only deletes tables where the join fails
			val target = targetFrom(rule.table, restrictions)
			val noJoinConditions = restrictions.map { _.last.to.column.isNull }
			
			connection(Delete(target, Vector(rule.table)) + Where(baseDeletionCondition && noJoinConditions))
		}
	}
	
	private def referencePathFrom(primaryTable: Table, childPath: Vector[Table]) =
	{
		var lastTable = primaryTable
		
		childPath.tryMap { nextTable =>
			val reference = References.fromTo(nextTable, lastTable).headOption.orElse { References.fromTo(lastTable,
				nextTable).headOption }.toTry { new NoReferenceFoundException(
				s"Can't find a reference between ${nextTable.name} and ${
					lastTable.name} even though there was supposed to be a reference there.") }
			lastTable = nextTable
			reference
		}
	}
	
	// Expects childPaths to be nonEmpty
	private def targetFrom(primaryTable: Table, childPaths: Iterable[Seq[Reference]]) =
	{
		val joins = childPaths.flatMap { _.map { reference => Join(reference.from.column, reference.to) } }
		joins.foldLeft(primaryTable: SqlTarget) { _ + _ }
	}
	
	
	// NESTED	-------------------------------
	
	private case class TableDeletionRule(table: Table, timePropertyName: String,
	                                     baseLiveDuration: Duration = Duration.Inf,
	                                     conditionalPeriods: Map[Condition, FiniteDuration] = Map(),
	                                     restrictiveChildPaths: Vector[Vector[Reference]] = Vector())
	{
		lazy val timeColumn = table(timePropertyName)
		
		lazy val restrictiveChildTables = restrictiveChildPaths.map { _.last.to.table }.toSet
		
		/*
		def maxDuration = baseLiveDuration.getOrElse(conditionalPeriods.values.max)
		
		def additionalConditionsRestricting(proposedDuration: Period) =
			conditionalPeriods.filter { _._2 > proposedDuration }.keys
			*/
	}
}