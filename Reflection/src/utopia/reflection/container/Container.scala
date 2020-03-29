package utopia.reflection.container

import utopia.reflection.component.ComponentLike

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
	 */
	protected def add(component: C): Unit
	
	/**
	 * Removes an item from this container
	 */
	protected def remove(component: C): Unit
}