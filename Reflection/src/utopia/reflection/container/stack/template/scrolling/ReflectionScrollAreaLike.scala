package utopia.reflection.container.stack.template.scrolling

import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackable}
import utopia.reflection.container.stack.template.StackContainerLike

/**
  * Scroll areas are containers that allow horizontal and / or vertical content scrolling
  * @author Mikko Hilpinen
  * @since 15.5.2019, v1+
  */
trait ReflectionScrollAreaLike[C <: ReflectionStackable]
	extends ScrollAreaLike2[C] with CachingReflectionStackable with StackContainerLike[C]
{
	// IMPLEMENTED	----------------
	
	override def components = Vector(content)
}
