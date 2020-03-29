package utopia.reflection.shape

import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.Bounds

/**
  * This class represents a set of bounds for scroll bar
  * @author Mikko Hilpinen
  * @since 15.5.2019, v1+
  * @param bar Bounds of the draggable scroll bar
  * @param area Bounds of the potential scroll bar area
  * @param axis Direction of scrollin (horizontal (X), or vertical (Y))
  */
case class ScrollBarBounds(bar: Bounds, area: Bounds, axis: Axis2D)
