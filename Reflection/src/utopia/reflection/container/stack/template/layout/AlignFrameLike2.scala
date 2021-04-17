package utopia.reflection.container.stack.template.layout

import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape1D.Direction1D.{Negative, Positive}
import utopia.genesis.shape.shape2D.{Bounds, Point}
import utopia.reflection.component.template.layout.stack.{CachingStackable2, Stackable2}
import utopia.reflection.container.template.SingleContainer2
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Center

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
		val targetSize = content.stackSize.optimal.fittedInto(mySize)
		
		// Calculates new position
		val align = alignment
		val targetPosition = Point.of(Axis2D.values.map { axis =>
			val myLength = mySize.along(axis)
			val targetLength = targetSize.along(axis)
			
			axis -> (align.directionAlong(axis) match
			{
				case Some(dir) =>
					dir match
					{
						case Positive => myLength - targetLength
						case Negative => 0
					}
				case None => (myLength - targetLength) / 2
			})
		}.toMap)
		
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
				if (align.directionAlong(axis).isDefined)
					length.noMax.expanding
				else
					length
			}
		}
	}
}
