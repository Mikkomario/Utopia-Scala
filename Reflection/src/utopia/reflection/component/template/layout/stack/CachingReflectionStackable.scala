package utopia.reflection.component.template.layout.stack

import utopia.firmament.component.stack.CachingStackable

/**
  * This stackable caches the calculated stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait CachingReflectionStackable extends CachingStackable with ReflectionStackable
{
	// ATTRIBUTES	-----------------
	
	override val stackId = hashCode()
	
	
	// ABSTRACT	---------------------
	
	/**
	  * Within this method the stackable instance should perform the actual visibility change
	  * @param visible Whether this stackable should become visible (true) or invisible (false)
	  */
	protected def updateVisibility(visible: Boolean): Unit
	
	
	// IMPLEMENTED	-----------------
	
	override def visible_=(isVisible: Boolean) = {
		// Revalidates this item each time visibility changes
		updateVisibility(isVisible)
		revalidate()
	}
}
