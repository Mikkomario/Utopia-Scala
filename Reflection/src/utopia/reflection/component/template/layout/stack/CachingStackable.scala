package utopia.reflection.component.template.layout.stack

import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.shape.stack.modifier.StackSizeModifier

/**
  * This stackable caches the calculated stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait CachingStackable extends Stackable with Constrainable
{
	// ATTRIBUTES	-----------------
	
	private var _constraints = Vector[StackSizeModifier]()
	private val cachedStackSize = ResettableLazy[StackSize] { calculatedStackSizeWithConstraints }
	
	
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
	
	override def visible_=(isVisible: Boolean) =
	{
		// Revalidates this item each time visibility changes
		updateVisibility(isVisible)
		revalidate()
	}
	
	override def stackSize = if (visible) cachedStackSize.value else StackSize.any
	
	override def resetCachedSize() = cachedStackSize.reset()
	
	override val stackId = hashCode()
}
