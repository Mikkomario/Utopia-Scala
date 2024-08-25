package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctReadModelAccess
import utopia.vault.nosql.factory.row.FromTimelineRowFactory
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.OrderDirection.{Ascending, Descending}
import utopia.vault.sql.{Condition, OrderDirection}

object LatestOrEarliestModelAccess
{
	// OTHER    -----------------------------
	
	/**
	  * Creates a new latest or earliest model access-point
	  * @param factory Factory to wrap
	  * @param direction Ordering direction to use.
	  *                  Determines whether targeting the latest or the earliest item
	  * @param condition Condition to apply over the whole access point
	  * @tparam A Type of accessed items
	  * @return A new access point using the specified factory and applying the specified filter condition
	  */
	def apply[A](factory: FromTimelineRowFactory[A], direction: OrderDirection,
	             condition: Option[Condition] = None): LatestOrEarliestModelAccess[A] =
		AccessWrapper[A](factory, direction, condition)
	
	/**
	  * Creates a new earliest model access-point
	  * @param factory Factory to wrap
	  * @param condition Condition to apply over the whole access point
	  * @tparam A Type of accessed items
	  * @return A new access point using the specified factory and applying the specified filter condition
	  */
	def earliest[A](factory: FromTimelineRowFactory[A], condition: Option[Condition] = None) =
		apply(factory, Ascending, condition)
	/**
	  * Creates a new latest model access-point
	  * @param factory Factory to wrap
	  * @param condition Condition to apply over the whole access point
	  * @tparam A Type of accessed items
	  * @return A new access point using the specified factory and applying the specified filter condition
	  */
	def latest[A](factory: FromTimelineRowFactory[A], condition: Option[Condition] = None) =
		apply(factory, Descending, condition)
	
	
	// NESTED   -----------------------------
	
	private case class AccessWrapper[+A](override val factory: FromTimelineRowFactory[A],
	                                     override val orderDirection: OrderDirection,
	                                     override val accessCondition: Option[Condition] = None)
		extends LatestOrEarliestModelAccess[A]
	{
		override protected def self = this
		
		override def apply(condition: Condition): LatestOrEarliestModelAccess[A] =
			copy(accessCondition = Some(condition))
	}
}

/**
  * Used for accessing the latest or the earliest model within a table or a subgroup
  * @author Mikko Hilpinen
  * @since 24.4.2024, v1.19
  */
trait LatestOrEarliestModelAccess[+A]
	extends SingleRowModelAccess[A] with DistinctReadModelAccess[A, Option[A], Value]
		with FilterableView[SingleRowModelAccess[A] with DistinctReadModelAccess[A, Option[A], Value]]
{
	// ABSTRACT ----------------------------
	
	// This access point requires a timestamp-based factory
	override def factory: FromTimelineRowFactory[A]
	
	/**
	  * @return Direction used when ordering items.
	  *         Determines whether latest (descending) or earliest (ascending) item is accessed
	  */
	protected def orderDirection: OrderDirection
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def defaultOrdering = Some(factory.directionalTimestampOrdering(orderDirection))
}
