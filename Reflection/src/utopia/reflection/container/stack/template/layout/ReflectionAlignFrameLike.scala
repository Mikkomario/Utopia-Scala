package utopia.reflection.container.stack.template.layout

import utopia.firmament.component.container.single.AlignFrameLike
import utopia.reflection.component.template.ReflectionComponentWrapper
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackable}
import utopia.reflection.container.stack.template.SingleStackContainer
import utopia.reflection.container.template.Container

/**
  * Contains a single item, which is aligned to a single side or corner, or the center
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
trait ReflectionAlignFrameLike[C <: ReflectionStackable]
	extends AlignFrameLike[C] with CachingReflectionStackable with SingleStackContainer[C]
		with ReflectionComponentWrapper
{
	// ABSTRACT	----------------------
	
	/**
	 * @return Container where the content is placed
	 */
	protected def container: Container[C]
	
	
	// IMPLEMENTED	------------------
	
	override def children = components
	
	override protected def wrapped = container
	
	override protected def updateVisibility(visible: Boolean) = super[CachingReflectionStackable].visible_=(visible)
	
	override def components = container.components
}
