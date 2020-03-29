package utopia.reflection.container.stack

import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.reflection.component.ComponentWrapper
import utopia.reflection.component.stack.{CachingStackable, Stackable}
import utopia.reflection.container.Container
import utopia.reflection.shape.{Alignment, StackSize}
import utopia.reflection.shape.Alignment.Center

/**
  * Contains a single item, which is aligned to a single side or corner, or the center
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1+
  */
trait AlignFrameLike[C <: Stackable] extends SingleStackContainer[C] with ComponentWrapper with CachingStackable
{
	// ABSTRACT	----------------------
	
	def alignment: Alignment
	
	def useLowPriorityLength: Boolean
	
	protected def container: Container[C]
	
	
	// IMPLEMENTED	------------------
	
	override def children = components
	
	override protected def wrapped = container
	
	override protected def updateVisibility(visible: Boolean) = super[CachingStackable].isVisible_=(visible)
	
	override def components = container.components
	
	// Aligns the component and fits it into this container's area
	override def updateLayout() =
	{
		content.foreach { c =>
			// Calculates new size
			val mySize = size
			val targetSize = c.stackSize.optimal.fittedInto(mySize)
			
			// Calculates new position
			val align = alignment
			val targetPosition = Point.of(Axis2D.values.map { axis =>
				val myLength = mySize.along(axis)
				val targetLength = targetSize.along(axis)
				
				axis -> (align.directionAlong(axis) match
				{
					case Some(dir) => if (dir.isPositiveDirection) myLength - targetLength else 0
					case None => (myLength - targetLength) / 2
				})
			}.toMap)
			
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
			{
				val noMax = c.stackSize.withNoMax
				if (useLowPriorityLength) noMax.withLowPriority else noMax
			}
			else
			{
				c.stackSize.map { (axis, length) =>
					if (align.directionAlong(axis).isDefined)
					{
						val noMax = length.noMax
						if (useLowPriorityLength) noMax.withLowPriority else noMax
					}
					else
						length
				}
			}
		}.getOrElse(StackSize.any)
	}
}
