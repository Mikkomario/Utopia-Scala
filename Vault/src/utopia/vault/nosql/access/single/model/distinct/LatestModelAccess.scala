package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctReadModelAccess
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.view.FilterableView
import utopia.vault.sql.Condition

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
	def apply[A](factory: FromRowFactoryWithTimestamps[A], condition: Option[Condition] = None) =
		new LatestModelAccessWrapper[A](factory, condition)
	
	
	// NESTED   -----------------------------
	
	/**
	  * A simple factory wrapper that provides access to the latest created model
	  * @param factory Factory to wrap
	  * @param accessCondition Global condition to apply (default = None)
	  * @tparam A Type of accessed items
	  */
	class LatestModelAccessWrapper[+A](override val factory: FromRowFactoryWithTimestamps[A],
	                                           override val accessCondition: Option[Condition] = None)
		extends LatestModelAccess[A] with FilterableView[LatestModelAccessWrapper[A]]
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
trait LatestModelAccess[+A] extends SingleRowModelAccess[A] with DistinctReadModelAccess[A, Option[A], Value]
{
	// COMPUTED ----------------------------
	
	// This access point requires a timestamp-based factory
	override def factory: FromRowFactoryWithTimestamps[A]
}
