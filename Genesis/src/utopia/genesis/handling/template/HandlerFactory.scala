package utopia.genesis.handling.template

import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.template.eventful.{Changing, FlagLike}

/**
  * Common trait for factory classes used for constructing handlers of some type.
  * Supports additional handling-conditions.
  *
  * @tparam A Type of handleable items
  * @tparam H Type of constructed handlers
  * @tparam Repr Type of this factory
  *
  * @author Mikko Hilpinen
  * @since 21/02/2024, v4.0
  */
trait HandlerFactory[-A, +H, +Repr] extends FromCollectionFactory[A, H]
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Condition that must be met for the items in this handler to be handled
	  */
	def condition: FlagLike = AlwaysTrue
	
	/**
	  * @param newCondition New handling-condition to apply.
	  *                     Overwrites any existing condition.
	  * @return Copy of this factory that uses the specified handling-condition instead of any existing condition.
	  */
	def usingCondition(newCondition: FlagLike): Repr
	
	/**
	  * @param initialItems The items to place on this handler, initially. Default = empty collection.
	  * @return A handler that contains the specified items
	  *         and applies the condition associated with this factory (if applicable)
	  */
	def apply(initialItems: IterableOnce[A]): H
	
	
	// IMPLEMENTED  ----------------------
	
	override def from(items: IterableOnce[A]): H = apply(items)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param condition Another condition to apply
	  * @return Copy of this factory that is only active while the specified condition is met.
	  *         Existing conditions are also respected, if applicable.
	  */
	def conditional(condition: Changing[Boolean]) = usingCondition(this.condition && condition)
}
