package utopia.reflection.container

import utopia.reflection.component.ComponentLike

/**
* This trait is extended by classes that may contain one or multiple components
* @author Mikko Hilpinen
* @since 25.3.2019
**/
trait MultiContainer[C <: ComponentLike] extends Container[C]
{
	// OPERATORS    ---------------
	
	/**
	  * Adds a new item to this container
	  */
	def +=(component: C) = add(component)
	
	/**
	  * Removes an item from this container
	  */
	def -=(component: C) = remove(component)
	
	/**
	 * Adds multiple items to this container
	 */
	def ++=(components: TraversableOnce[C]) = components.foreach(+=)
	
	/**
	 * Adds multiple items to this container
	 */
	def ++=(first: C, second: C, more: C*): Unit = ++=(Vector(first, second) ++ more)
	
	/**
	 * Removes multiple items from this container
	 */
	def --=(components: TraversableOnce[C]) = components.foreach(-=)
	
	/**
	 * Removes multiple items from this container
	 */
	def --=(first: C, second: C, more: C*): Unit = --=(Vector(first, second) ++ more)
	
	
	// OTHER    -------------------
	
	/**
	 * Removes all items from this container
	 */
	def clear() = components.foreach(-=)
	
	/**
	  * Removes all items except those kept by the filter
	  * @param keep A filter for keeping items
	  */
	def filter(keep: C => Boolean) = this --= components.filterNot(keep)
	
	/**
	  * Removes all items picked by the filter function
	  * @param f A filter function
	  */
	def filterNot(f: C => Boolean) = this --= components.filter(f)
}