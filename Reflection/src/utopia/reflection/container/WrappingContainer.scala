package utopia.reflection.container

import utopia.reflection.component.{ComponentLike, ComponentWrapper}

/**
  * This container wraps components and holds them in another another container. Presents itself still as a container
  * of the original items.
  * @author Mikko Hilpinen
  * @since 20.4.2020, v1.2
  */
trait WrappingContainer[C <: ComponentLike, Wrap <: ComponentLike, Wrapped <: ComponentLike] extends Container[C] with ComponentWrapper
{
	// ABSTRACT	------------------------------
	
	/**
	  * @return The container used by this container to hold the wrapped content
	  */
	protected def container: Wrapped
	
	/**
	  * @return Currently used wrappers, along with their wrapped components
	  */
	protected def wrappers: Vector[(Wrap, C)]
	
	/**
	  * @param wrapper Wrapper to remove
	  * @param component Component to remove along with the wrapper
	  */
	protected def removeWrapper(wrapper: Wrap, component: C): Unit
	
	
	// IMPLEMENTED	--------------------------
	
	override protected def remove(component: C) = wrappers.find { _._2 == component }.foreach { case (w, c) => removeWrapper(w, c) }
	
	override protected def wrapped = container
	
	override def components = wrappers.map { _._2 }
}
