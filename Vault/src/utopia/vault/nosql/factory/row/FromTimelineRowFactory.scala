package utopia.vault.nosql.factory.row

import utopia.flow.collection.immutable.Empty
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.{ComparisonOperator, SelectTarget}
import utopia.vault.model.enumeration.ComparisonOperator.{Larger, LargerOrEqual, Smaller, SmallerOrEqual}
import utopia.vault.model.immutable.{DbPropertyDeclaration, Row, Table}
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.OrderDirection.Descending
import utopia.vault.sql.{Condition, JoinType, OrderBy, OrderDirection}

import java.time.Instant
import scala.util.Try

object FromTimelineRowFactory
{
	// OTHER    -------------------------
	
	/**
	  * Attaches timeline features to another factory
	  * @param factory Factory to wrap
	  * @param timestamp Timestamp property to use
	  * @tparam A Type of items accessed
	  * @return A wrapper factory that provides timeline features
	  */
	def wrap[A](factory: FromRowFactory[A], timestamp: DbPropertyDeclaration): FromTimelineRowFactory[A] =
		new FactoryWrapper[A](factory, timestamp)
	
	
	// NESTED   -------------------------
	
	private class FactoryWrapper[+A](factory: FromRowFactory[A], override val timestamp: DbPropertyDeclaration)
		extends FromTimelineRowFactory[A]
	{
		
		override def table: Table = factory.table
		override def joinedTables: Seq[Table] = factory.joinedTables
		override def joinType: JoinType = factory.joinType
		
		override def selectTarget: SelectTarget = factory.selectTarget
		
		override def apply(row: Row): Try[A] = factory(row)
	}
}

/**
  * A common trait for factories that track row creation time
  * @author Mikko Hilpinen
  * @since 1.2.2020, v1.4
  */
trait FromTimelineRowFactory[+A] extends FromRowFactory[A]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Declaration for the timestamp property
	  */
	def timestamp: DbPropertyDeclaration
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return A descending timestamp-based ordering.
	  *         I.e. an ordering that returns items from the latest to the earliest.
	  */
	def timestampOrdering = directionalTimestampOrdering(Descending)
	
	/**
	  * @return The latest recorded item
	  */
	def latest(implicit connection: Connection) = maxBy(timestamp.column)
	/**
	  * @return The earliest recorded item
	  */
	def earliest(implicit connection: Connection) = minBy(timestamp.column)
	
	
	// IMPLEMENTED	---------------------
	
	/**
	  * @return Default ordering used by this factory (by default returns first the latest items)
	  */
	override def defaultOrdering: Option[OrderBy] = Some(timestampOrdering)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param direction Desired ordering direction
	  * @return A row timestamp -based ordering with the specified ordering direction
	  */
	def directionalTimestampOrdering(direction: OrderDirection) = OrderBy(timestamp, direction)
	
	/**
	  * @param where      A search condition
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use in joins (default = inner)
	  * @param connection DB Connection (implicit)
	  * @return Latest item in database that satisfies the specified condition
	  */
	def findLatest(where: Condition, joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)
	              (implicit connection: Connection) =
		findMaxBy(timestamp.column, where, joins, joinType)
	/**
	  * @param where      A search condition
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use in joins (default = inner)
	  * @param connection DB Connection (implicit)
	  * @return Earliest item in database that satisfies the specified condition
	  */
	def findEarliest(where: Condition, joins: Seq[Joinable] = Empty, joinType: JoinType = Inner)
	                (implicit connection: Connection) =
		findMinBy(timestamp.column, where, joins, joinType)
	
	/**
	  * Takes latest n items from the database
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param connection       DB Connection (implicit)
	  * @return Latest 'maxNumberOfItems' items from database
	  */
	def takeLatest(maxNumberOfItems: Int)(implicit connection: Connection) =
		take(maxNumberOfItems, directionalTimestampOrdering(Descending))
	
	/**
	  * Finds a specific number of latest items that satisfy specified search condition
	  * @param condition        A search condition
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param connection       DB Connection
	  * @return Latest 'maxNumberOfItems' items that satisfy specified condition
	  */
	def takeLatestWhere(condition: Condition, maxNumberOfItems: Int)(implicit connection: Connection) =
		take(maxNumberOfItems, directionalTimestampOrdering(Descending), Some(condition))
	
	/**
	  * @param threshold Time threshold
	  * @param operator  An operator used for comparing row timestamps with specified threshold
	  * @return A condition that accepts rows based on specified threshold and operator
	  */
	def timeCondition(threshold: Instant, operator: ComparisonOperator) =
		timestamp.column.makeCondition(operator, threshold)
	/**
	  * @param threshold   Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows where the timestamp is before the specified time threshold
	  */
	def beforeCondition(threshold: Instant, isInclusive: Boolean = false) =
		timeCondition(threshold, if (isInclusive) SmallerOrEqual else Smaller)
	/**
	  * @param threshold   Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows where the timestamp is after the specified time threshold
	  */
	def afterCondition(threshold: Instant, isInclusive: Boolean = false) =
		timeCondition(threshold, if (isInclusive) LargerOrEqual else Larger)
	/**
	  * @param start Minimum time
	  * @param end   Maximum time
	  * @return A condition that accepts items where the timestamp is between 'start' and 'end'
	  */
	def betweenCondition(start: Instant, end: Instant) = timestamp.isBetween(start, end)
	
	/**
	  * @param threshold           Time threshold
	  * @param maxNumberOfItems    Maximum number of items to return
	  * @param isInclusive         Whether the threshold should be included in return values (default = false)
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @return Up to 'maxNumberOfItems' items where the timestamp is before the specified time threshold
	  */
	def before(threshold: Instant, maxNumberOfItems: Int, isInclusive: Boolean = false,
	           additionalCondition: Option[Condition] = None)
	          (implicit connection: Connection) =
	{
		val condition = beforeCondition(threshold, isInclusive) && additionalCondition
		take(maxNumberOfItems, directionalTimestampOrdering(Descending), Some(condition))
	}
	/**
	  * @param threshold           Time threshold
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @param isInclusive         Whether the threshold should be included in return values (default = false)
	  * @param connection          DB Connection (implicit)
	  * @return All items where the timestamp is after the specified time threshold
	  */
	def after(threshold: Instant, additionalCondition: Option[Condition] = None, isInclusive: Boolean = false)
	                (implicit connection: Connection) =
	{
		val condition = afterCondition(threshold, isInclusive) && additionalCondition
		findMany(condition)
	}
	
	/**
	  * @param threshold   A time threshold
	  * @param isInclusive Whether an exactly matching timestamp should be included (default = false)
	  * @param connection  DB Connection (implicit)
	  * @return The last item where the timestamp is before the specified time threshold (None if no items were found)
	  */
	def latestBefore(threshold: Instant, isInclusive: Boolean = false)(implicit connection: Connection) =
		find(beforeCondition(threshold, isInclusive))
	/**
	  * @param threshold   A time threshold
	  * @param isInclusive Whether an exactly matching timestamp should be included (default = false)
	  * @param connection  DB Connection (implicit)
	  * @return The first item where the timestamp is after the specified time threshold (None if no items were found)
	  */
	def firstAfter(threshold: Instant, isInclusive: Boolean = false)(implicit connection: Connection) =
		find(afterCondition(threshold, isInclusive))
}
