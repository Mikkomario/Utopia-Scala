package utopia.reflection.container.stack.template.layout

import utopia.paradigm.shape.shape2d.Bounds
import utopia.reflection.component.template.layout.stack.{CachingStackable2, Stackable2}
import utopia.reflection.container.template.SingleContainer2
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center

/**
  * Contains a single item, which is aligned to a single side or corner, or the center
  * @author Mikko Hilpinen
  * @since 17.12.2019, v1
  */
trait AlignFrameLike2[+C <: Stackable2] extends SingleContainer2[C] with CachingStackable2
{
	// ABSTRACT	----------------------
	
	/**
	 * @return Alignment used in content alignment
	 */
	def alignment: Alignment
	
	
	// IMPLEMENTED	------------------
	
	// Aligns the component and fits it into this container's area
	override def updateLayout() =
	{
		// Calculates new size
		val mySize = size
		val targetSize = content.stackSize.optimal.croppedToFit(mySize)
		
		// Calculates new position
		val targetPosition = alignment.position(targetSize, mySize)
		
		// Updates component bounds
		content.bounds = Bounds(targetPosition, targetSize)
	}
	
	override def calculatedStackSize =
	{
		// Uses content's stack size as base, but doesn't have a maximum limit on alignable axis / axes
		// May also use low priority for said axis
		val align = alignment
		if (align == Center)
			content.stackSize.withNoMax.expanding
		else
		{
			content.stackSize.map { (axis, length) =>
				if (align.along(axis).movesItems)
					length.noMax.expanding
				else
					length
			}
		}
	}
}
