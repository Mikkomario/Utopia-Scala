package utopia.firmament.component.container.single

import utopia.firmament.component.stack.{CachingStackable, Stackable}
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Alignment.Center
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds

/**
  * Contains a single item, which is aligned to a single side or corner, or the center
  * @author Mikko Hilpinen
  * @since 17.12.2019, Reflection v1
  */
trait AlignFrameLike[+C <: Stackable] extends CachingStackable with SingleContainer[C]
{
	// ABSTRACT	----------------------
	
	/**
	 * @return Alignment used in content alignment
	 */
	def alignment: Alignment
	/**
	  * @return Whether the wrapped component should be scaled to fill this container's space along the non-aligned axis.
	  */
	def scaleToFill: Boolean
	
	
	// IMPLEMENTED	------------------
	
	// Aligns the component and fits it into this container's area
	override def updateLayout() = {
		// Calculates new size
		val mySize = size
		val optimalContentSize = content.stackSize.optimal
		val alignment = this.alignment
		val targetSize = {
			// Case: Scales to fill the "breadth" of this container
			if (scaleToFill)
				mySize.mapComponents { myLength =>
					if (alignment(myLength.axis).movesItems)
						optimalContentSize(myLength.axis) min myLength.length
					else
						myLength.length
				}
			// Case: Component is not scaled beyond its optimal size
			else
				optimalContentSize.croppedToFitWithin(mySize)
		}
		// Calculates new position
		val targetPosition = alignment.position(targetSize, mySize)
		
		// Updates component bounds
		content.bounds = Bounds(targetPosition, targetSize)
	}
	
	override def calculatedStackSize = {
		// Uses content's stack size as base, but doesn't have a maximum limit on alignable axis / axes
		// May also use low priority for said axis
		val align = alignment
		if (align == Center)
			content.stackSize.withNoMax.expanding
		else
			content.stackSize.map { (axis, length) =>
				if (align(axis).movesItems)
					length.noMax.expanding
				else
					length
			}
	}
}
