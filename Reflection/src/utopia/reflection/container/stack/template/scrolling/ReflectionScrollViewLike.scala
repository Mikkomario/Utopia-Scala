package utopia.reflection.container.stack.template.scrolling

import utopia.reflection.component.template.layout.stack.ReflectionStackable

/**
  * Scroll views are containers that allow horizontal or vertical content scrolling
  * @author Mikko Hilpinen
  * @since 30.4.2019, v1+
  */
trait ReflectionScrollViewLike[C <: ReflectionStackable] extends ScrollViewLike2[C] with ReflectionScrollAreaLike[C]
{
	override def children = components
}