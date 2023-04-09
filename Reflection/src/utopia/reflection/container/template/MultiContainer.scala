package utopia.reflection.container.template

import utopia.flow.collection.CollectionExtensions._
import utopia.reflection.component.template.ComponentLike

/**
* This trait is extended by classes that may contain one or multiple components
* @author Mikko Hilpinen
* @since 25.3.2019
**/
@deprecated("Replaced with a new version", "v2.0")
trait MultiContainer[C <: ComponentLike] extends Container[C]
{
	// OPERATORS    ---------------
	
	/**
	  * Adds a new item to this container
	  */
	def +=(component: C, index: Int = components.size) = insert(component, index)
	
	/**
	  * Removes an item from this container
	  */
	def -=(component: C) = remove(component)
	
	/**
	 * Adds multiple items to this container
	 */
	def ++=(components: IterableOnce[C]) = components.iterator.foreach { this += _ }
	
	/**
	 * Adds multiple items to this container
	 */
	def ++=(first: C, second: C, more: C*): Unit = ++=(Vector(first, second) ++ more)
	
	/**
	 * Removes multiple items from this container
	 */
	def --=(components: IterableOnce[C]) = components.iterator.foreach(-=)
	
	/**
	 * Removes multiple items from this container
	 */
	def --=(first: C, second: C, more: C*): Unit = --=(Vector(first, second) ++ more)
	
	
	// OTHER    -------------------
	
	/**
	  * Inserts a component to a specific position
	  * @param component Component to add
	  * @param index Index where the component should be added
	  */
	def insert(component: C, index: Int) = add(component, index)
	
	/**
	  * Inserts multiple components to a specific position
	  * @param components Components to add
	  * @param index Index where the components should be added
	  */
	def insertMany(components: Seq[C], index: Int) = components.foreachWithIndex { (c, i) => insert(c, index + i) }
	
	/**
	  * Removes specified range of components from this container
	  * @param range Range of indices to remove
	  * @return Components that were removed from this container
	  */
	def removeComponentsIn(range: Range) =
	{
		val componentsToRemove = components.slice(range)
		componentsToRemove.reverseIterator.foreach(-=)
		componentsToRemove
	}
	
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
