package utopia.vault.nosql.factory.row

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator
import utopia.vault.model.enumeration.ComparisonOperator.{Larger, LargerOrEqual, Smaller, SmallerOrEqual}
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vault.sql.Condition
import utopia.vault.sql.OrderDirection.Descending

import java.time.Instant

/**
  * A common trait for factories that track row creation time
  * @author Mikko Hilpinen
  * @since 1.2.2020, v1.4
  */
@deprecated("Please migrate to using FromTimelineRowFactory instead", "v1.19")
trait FromRowFactoryWithTimestamps[+A] extends FromTimelineRowFactory[A]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Name of the property that represents item creation time
	  */
	def creationTimePropertyName: String
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Column that specifies row creation time
	  */
	def creationTimeColumn = table(creationTimePropertyName)
	/**
	  * @return Ordering that uses row creation time (descending)
	  */
	@deprecated("Please use timestampOrdering instead", "v1.19")
	def creationTimeOrdering = timestampOrdering
	
	
	// IMPLEMENTED	---------------------
	
	override def timestamp: DbPropertyDeclaration = DbPropertyDeclaration.from(table, creationTimePropertyName)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param threshold Time threshold
	  * @param operator  An operator used for comparing row creation times with specified threshold
	  * @return A condition that accepts rows based on specified threshold and operator
	  */
	@deprecated("Please use timeCondition(...) instead", "v1.19")
	def creationCondition(threshold: Instant, operator: ComparisonOperator) =
		timestamp.column.makeCondition(operator, threshold)
	/**
	  * @param threshold   Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows that were created before the specified time threshold
	  */
	@deprecated("Please use beforeCondition(...) instead", "v1.19")
	def createdBeforeCondition(threshold: Instant, isInclusive: Boolean = false) =
		creationCondition(threshold, if (isInclusive) SmallerOrEqual else Smaller)
	/**
	  * @param threshold   Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows that were created after the specified time threshold
	  */
	@deprecated("please use afterCondition(...) instead", "v1.19")
	def createdAfterCondition(threshold: Instant, isInclusive: Boolean = false) =
		creationCondition(threshold, if (isInclusive) LargerOrEqual else Larger)
	/**
	  * @param start Minimum creation time
	  * @param end   Maximum creation time
	  * @return A condition that accepts items that were created between 'start' and 'end'
	  */
	@deprecated("Please use betweenCondition(...) instead", "v1.19")
	def createdBetweenCondition(start: Instant, end: Instant) = timestamp.isBetween(start, end)
	
	/**
	  * @param threshold           Time threshold
	  * @param maxNumberOfItems    Maximum number of items to return
	  * @param isInclusive         Whether the threshold should be included in return values (default = false)
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @return Up to 'maxNumberOfItems' items that were created before the specified time threshold
	  */
	@deprecated("Please use before(...) instead", "v1.19")
	def createdBefore(threshold: Instant, maxNumberOfItems: Int, isInclusive: Boolean = false,
	                  additionalCondition: Option[Condition] = None)(implicit connection: Connection) =
	{
		val condition = createdBeforeCondition(threshold, isInclusive) && additionalCondition
		take(maxNumberOfItems, directionalTimestampOrdering(Descending), Some(condition))
	}
	
	/**
	  * @param threshold           Time threshold
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @param isInclusive         Whether the threshold should be included in return values (default = false)
	  * @param connection          DB Connection (implicit)
	  * @return All items that were created after the specified time threshold
	  */
	@deprecated("Please use after(...) instead", "v1.19")
	def createdAfter(threshold: Instant, additionalCondition: Option[Condition] = None, isInclusive: Boolean = false)
	                (implicit connection: Connection) =
	{
		val condition = createdAfterCondition(threshold, isInclusive) && additionalCondition
		findMany(condition)
	}
}
