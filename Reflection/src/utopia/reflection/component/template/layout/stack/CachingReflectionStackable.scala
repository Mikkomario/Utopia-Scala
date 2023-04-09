package utopia.reflection.component.template.layout.stack

/**
  * This stackable caches the calculated stack size
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
abstract class CachingReflectionStackable extends CachingStackable2 with ReflectionStackable
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
