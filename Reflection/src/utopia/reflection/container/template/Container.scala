package utopia.reflection.container.template

import utopia.reflection.component.template.ComponentLike

/**
* Common trait for mutable component containers / hierarchies
* @author Mikko Hilpinen
* @since 25.3.2019
**/
trait Container[C <: ComponentLike] extends ContainerLike[C]
{
    // ABSTRACT    ----------------
    
	/**
	 * Adds a new item to this container
	  * @param component Component to add
	  * @param index Index where the component should be added
	 */
	protected def add(component: C, index: Int): Unit
	
	/**
	 * Removes an item from this container
	 */
	protected def remove(component: C): Unit
}
