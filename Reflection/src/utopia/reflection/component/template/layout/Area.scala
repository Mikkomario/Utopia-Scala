package utopia.reflection.component.template.layout

import utopia.genesis.shape.Axis.{X, Y}
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.genesis.shape.shape3D.Vector3D

/**
* This trait is extended by classes that occupy a certain 2D space (position + size)
* @author Mikko Hilpinen
* @since 26.2.2019
**/
trait Area
{
    // ABSTRACT    ------------------------
    
    /**
      * @return The position of this component's top-left corner
      */
    def position: Point
    def position_=(p: Point): Unit
    
    /**
      * @return The current size of this component
      */
    def size: Size
    def size_=(s: Size): Unit
    
    /**
      * @return The current bounds (position and size) of this component
      */
    def bounds: Bounds
    def bounds_=(b: Bounds): Unit
    
    
    // COMPUTED    -----------------------
    
    def position_+=(adjustment: Vector3D) = position = position + adjustment
    def position_-=(adjustment: Vector3D) = position += (-adjustment)
    
    def size_+=(adjustment: Size) = size = size + adjustment
    def size_-=(adjustment: Size) = size += (-adjustment)
    
	def x = position.x
    def x_=(newX: Double) = position = position.withX(newX)
    def x_+=(adjustment: Double) = position = position + X(adjustment)
    def x_-=(adjustment: Double) = x += (-adjustment)
    
    def y = position.y
    def y_=(newY: Double) = position = position.withY(newY)
    def y_+=(adjustment: Double) = position = position + Y(adjustment)
    def y_-=(adjustment: Double) = y += (-adjustment)
    
    def width = size.width
    def width_=(w: Double) = size = size.withWidth(w)
    def width_+=(adjustment: Double) = size = size + X(adjustment)
    def width_-=(adjustment: Double) = width += (-adjustment)
    
    def height = size.height
    def height_=(h: Double) = size = size.withHeight(h)
    def height_+=(adjustment: Double) = size = size + Y(adjustment)
    def height_-=(adjustment: Double) = height += (-adjustment)
    
    def rightX = x + width
    def rightX_=(newX: Double) = x = newX - width
    def bottomY = y + height
    def bottomY_=(newY: Double) = y = newY - height
    
    def topLeft = position
    def topLeft_=(p: Point) = position = p
    def topRight = position.plusX(width)
    def topRight_=(p: Point) = position = p.minusX(width)
    def bottomRight = position + size
    def bottomRight_=(p: Point) = position = p - size
    def bottomLeft = position.plusY(height)
    def bottomLeft_=(p: Point) = position = p.minusY(height)
    def center = position + size / 2
    def center_=(p: Point) = position = p - size / 2
    
    
    // OTHER    ------------------------
    
    /**
     * The x or y coordinate of this component
     */
    def coordinateAlong(axis: Axis2D) = axis match
    {
        case X => x
        case Y => y
    }
    
    /**
      * @param axis Target axis
      * @return The right side x-coordinate or the bottom y-coordinate, depending on target axis
      */
    def maxCoordinateAlong(axis: Axis2D) = axis match
    {
        case X => rightX
        case Y => bottomY
    }
    
    /**
     * Changes either x- or y-coordinate of this area
     * @param position the target coordinate
     * @param axis the target axis (X or Y)
     */
    def setCoordinate(position: Double, axis: Axis2D) = axis match
    {
        case X => x = position
        case Y => y = position
    }
    
    /**
     * Adjusts either x- or y-coordinate of this area
     */
    def adjustCoordinate(adjustment: Double, axis: Axis2D) = axis match
    {
        case X => x += adjustment
        case Y => y += adjustment
    }
    
    /**
     * The length of this component along the specified axis
     */
    def lengthAlong(axis: Axis2D) = axis match
    {
        case X => width
        case Y => height
    }
    
    /**
     * Changes either the width or height of this area
     * @param length the new side length
     * @param axis the target axis (X for width, Y for height)
     */
    def setLength(length: Double, axis: Axis2D) = axis match
    {
        case X => width = length
        case Y => height = length
    }
    
    /**
     * Adjusts either the width (for X-axis) or height (for Y-axis) of this component
     */
    def adjustLength(adjustment: Double, axis: Axis2D) = axis match
    {
        case X => width += adjustment
        case Y => height += adjustment
    }
}
