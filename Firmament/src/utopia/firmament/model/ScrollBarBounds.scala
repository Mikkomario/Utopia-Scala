package utopia.firmament.model

import utopia.flow.operator.Combinable
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.Bounds
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

/**
  * This class represents a set of bounds for scroll bar
  * @author Mikko Hilpinen
  * @since 15.5.2019, Reflection v1+
  * @param bar Bounds of the draggable scroll bar. Relative to the draw origin (i.e. parent component top-left corner)
  * @param area Bounds of the potential scroll bar area. Relative to the draw origin (i.e. parent component top-left corner)
  * @param axis Direction of scrolling (horizontal (X), or vertical (Y))
  */
case class ScrollBarBounds(bar: Bounds, area: Bounds, axis: Axis2D)
	extends Combinable[HasDoubleDimensions, ScrollBarBounds]
{
	override def +(other: HasDoubleDimensions) = copy(bar = bar + other, area = area + other)
}