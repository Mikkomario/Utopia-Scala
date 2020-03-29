package utopia.reflection.component.stack

import utopia.flow.datastructure.mutable.Lazy
import utopia.reflection.shape.{StackSize, StackSizeModifier}

/**
  * This stackable caches the calculated stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait CachingStackable extends Stackable with Constrainable
{
	// ATTRIBUTES	-----------------
	
	private var _constraints = Vector[StackSizeModifier]()
	private val cachedStackSize = Lazy[StackSize] { calculatedStackSizeWithConstraints }
	
	
	// ABSTRACT	---------------------
	
	/**
	  * Within this method the stackable instance should perform the actual visibility change
	  * @param visible Whether this stackable should become visible (true) or invisible (false)
	  */
	protected def updateVisibility(visible: Boolean): Unit
	
	
	// IMPLEMENTED	-----------------
	
	override def constraints = _constraints
	
	override def constraints_=(newConstraints: Vector[StackSizeModifier]) =
	{
		// Requires revalidation after constraints update
		_constraints = newConstraints
		revalidate()
	}
	
	override def isVisible_=(isVisible: Boolean) =
	{
		// Revalidates this item each time visibility changes
		updateVisibility(isVisible)
		revalidate()
	}
	
	override def stackSize = if (isVisible) cachedStackSize.get else StackSize.any
	
	override def resetCachedSize() = cachedStackSize.reset()
	
	override def stackId = hashCode()
}
