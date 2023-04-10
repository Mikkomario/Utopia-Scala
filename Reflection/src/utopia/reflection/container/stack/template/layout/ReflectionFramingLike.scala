package utopia.reflection.container.stack.template.layout

import utopia.reflection.component.template.ReflectionComponentWrapper
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackable}
import utopia.reflection.container.stack.template.SingleStackContainer
import utopia.reflection.container.template.Container

/**
  * Framings are containers that present a component with scaling 'frames', like a painting
  * @author Mikko Hilpinen
  * @since 26.4.2019, v1+
  */
trait ReflectionFramingLike[C <: ReflectionStackable] extends FramingLike2[C] with SingleStackContainer[C]
	with ReflectionComponentWrapper with CachingReflectionStackable
{
	// ABSTRACT	-----------------------
	
	/**
	  * @return The underlying container of this framing
	  */
	protected def container: Container[C]
	
	
	// IMPLEMENTED	--------------------
	
	override def children = components
	
	override protected def wrapped = container
	
	override def visible_=(isVisible: Boolean) = super[CachingReflectionStackable].visible_=(isVisible)
	
	override protected def updateVisibility(visible: Boolean) = super[ReflectionComponentWrapper].visible_=(visible)
	
	override def components = container.components
}
