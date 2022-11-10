package utopia.reflection.container.stack.template.layout

import utopia.paradigm.shape.shape2d.Bounds
import utopia.reflection.component.template.ComponentWrapper
import utopia.reflection.component.template.layout.stack.{CachingStackable, Stackable}
import utopia.reflection.container.stack.template.SingleStackContainer
import utopia.reflection.container.template.Container
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackSize

/**
  * Contains a single item, which is aligned to a single side or corner, or the center
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
trait AlignFrameLike[C <: Stackable] extends SingleStackContainer[C] with ComponentWrapper with CachingStackable
{
	// ABSTRACT	----------------------
	
	/**
	 * @return Alignment used in content alignment
	 */
	def alignment: Alignment
	
	/**
	 * @return Container where the content is placed
	 */
	protected def container: Container[C]
	
	
	// IMPLEMENTED	------------------
	
	override def children = components
	
	override protected def wrapped = container
	
	override protected def updateVisibility(visible: Boolean) = super[CachingStackable].visible_=(visible)
	
	override def components = container.components
	
	// Aligns the component and fits it into this container's area
	override def updateLayout() = {
		content.foreach { c =>
			// Calculates new size
			val mySize = size
			val targetSize = c.stackSize.optimal.croppedToFitWithin(mySize)
			
			// Calculates new position
			val targetPosition = alignment.position(targetSize, mySize)
			
			// Updates component bounds
			c.bounds = Bounds(targetPosition, targetSize)
		}
	}
	
	override def calculatedStackSize =
	{
		content.map { c =>
			// Uses content's stack size as base, but doesn't have a maximum limit on alignable axis / axes
			// May also use low priority for said axis
			val align = alignment
			if (align == Center)
				c.stackSize.withNoMax.expanding
			else
				c.stackSize.map { (axis, length) =>
					if (align(axis).movesItems)
						length.noMax.expanding
					else
						length
				}
		}.getOrElse(StackSize.any)
	}
}
