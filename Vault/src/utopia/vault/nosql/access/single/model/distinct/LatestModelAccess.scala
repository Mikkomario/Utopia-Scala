package utopia.vault.nosql.access.single.model.distinct

import utopia.vault.nosql.factory.row.FromTimelineRowFactory
import utopia.vault.sql.OrderDirection.Descending
import utopia.vault.sql.{Condition, OrderDirection}

object LatestModelAccess
{
	// OTHER    -----------------------------
	
	/**
	  * Creates a new latest model access-point
	  * @param factory Factory to wrap
	  * @param condition Condition to apply over the whole access point
	  * @tparam A Type of accessed items
	  * @return A new access point using the specified factory and applying the specified filter condition
	  */
	def apply[A](factory: FromTimelineRowFactory[A], condition: Option[Condition] = None) =
		LatestOrEarliestModelAccess.latest(factory, condition)
	
	
	// NESTED   -----------------------------
	
	/**
	  * A simple factory wrapper that provides access to the latest created model
	  * @param factory Factory to wrap
	  * @param accessCondition Global condition to apply (default = None)
	  * @tparam A Type of accessed items
	  */
	@deprecated("Deprecated for removal. Please use LatestOrEarliestModelAccess instead", "v1.19")
	class LatestModelAccessWrapper[+A](override val factory: FromTimelineRowFactory[A],
	                                           override val accessCondition: Option[Condition] = None)
		extends LatestModelAccess[A]
	{
		override protected def self = this
		
		override def filter(additionalCondition: Condition) =
			new LatestModelAccessWrapper(factory, Some(mergeCondition(additionalCondition)))
	}
}

/**
  * Used for accessing the latest model within a table or a sub-group
  * @author Mikko Hilpinen
  * @since 17.6.2021, v1.8
  */
trait LatestModelAccess[+A] extends LatestOrEarliestModelAccess[A]
{
	override protected def orderDirection: OrderDirection = Descending
}
