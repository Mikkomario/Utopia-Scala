package utopia.reflection.container.template.mutable

import utopia.flow.util.CollectionExtensions._
import utopia.reflection.component.template.ComponentLike2
import utopia.reflection.container.template.MultiContainer2

/**
* This trait is extended by classes that may contain one or multiple components
* @author Mikko Hilpinen
* @since 25.3.2019
  * @tparam A Type of items added to this container
  * @tparam C Type of items held in this container
**/
trait MutableMultiContainer2[-A, C <: ComponentLike2] extends MultiContainer2[C]
{
	// ABSTRACT	-------------------
	
	/**
	  * Adds a new item to this container
	  * @param component Component to add
	  * @param index Index where the component should be added
	  */
	protected def add(component: A, index: Int): Unit
	
	/**
	  * Adds a number of new items to this container
	  * @param components Components to add
	  * @param index Index where the components should be added
	  */
	protected def add(components: IterableOnce[A], index: Int): Unit
	
	/**
	  * Removes an item from this container
	  * @param component component to remove
	  */
	protected def remove(component: C): Unit
	
	/**
	  * Removes a number of items from this container
	  * @param components Components to remove
	  */
	protected def remove(components: IterableOnce[C]): Unit
	
	
	// OPERATORS    ---------------
	
	/**
	  * Adds a new item to this container
	  */
	def +=(component: A, index: Int = components.size) = insert(component, index)
	
	/**
	  * Removes an item from this container
	  */
	def -=(component: C) = remove(component)
	
	/**
	 * Adds multiple items to this container
	 */
	def ++=(newComponents: IterableOnce[A]) = insertMany(newComponents, components.size)
	
	/**
	 * Adds multiple items to this container
	 */
	def ++=(first: A, second: A, more: A*): Unit = ++=(Vector(first, second) ++ more)
	
	/**
	 * Removes multiple items from this container
	 */
	def --=(components: IterableOnce[C]) = remove(components)
	
	/**
	 * Removes multiple items from this container
	 */
	def --=(first: C, second: C, more: C*): Unit = --=(Set(first, second) ++ more)
	
	
	// OTHER    -------------------
	
	/**
	  * Inserts a component to a specific position
	  * @param component Component to add
	  * @param index Index where the component should be added
	  */
	def insert(component: A, index: Int) = add(component, index)
	
	/**
	  * Inserts multiple components to a specific position
	  * @param components Components to add
	  * @param index Index where the components should be added
	  */
	def insertMany(components: IterableOnce[A], index: Int) = add(components, index)
	
	/**
	  * Removes specified range of components from this container
	  * @param range Range of indices to remove
	  * @return Components that were removed from this container
	  */
	def removeComponentsIn(range: Range) =
	{
		val componentsToRemove = components.slice(range)
		remove(componentsToRemove)
		componentsToRemove
	}
	
	/**
	 * Removes all items from this container
	 */
	def clear() = remove(components)
	
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
