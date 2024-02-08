package utopia.firmament.component

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.{Bounds, HasBounds}
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

object HasMutableBounds
{
    // OTHER    ---------------------
    
    /**
      * @param initialBounds A set of bounds to wrap initially
      * @return A mutable bounds-wrapper
      */
    def apply(initialBounds: Bounds = Bounds.zero): HasMutableBounds = new MutableBoundsWrapper(initialBounds)
    
    
    // NESTED   ---------------------
    
    private class MutableBoundsWrapper(initial: Bounds) extends HasMutableBounds
    {
        // ATTRIBUTES   -------------
        
        override var bounds = initial
        
        
        // IMPLEMENTED  -------------
        
        override def position_=(p: Point): Unit = bounds = bounds.withPosition(p)
        override def size_=(s: Size): Unit = bounds = bounds.withSize(s)
    }
}

/**
* This trait is extended by classes that occupy a certain 2D space (position + size)
* @author Mikko Hilpinen
* @since 26.2.2019
**/
// TODO: Extend HasMutableSize
trait HasMutableBounds extends HasBounds
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
    
    def width_=(w: Double) = size = size.withWidth(w)
    def width_+=(adjustment: Double) = size = size + X(adjustment)
    def width_-=(adjustment: Double) = width += (-adjustment)
    
    def height_=(h: Double) = size = size.withHeight(h)
    def height_+=(adjustment: Double) = size = size + Y(adjustment)
    def height_-=(adjustment: Double) = height += (-adjustment)
    
    def rightX_=(newX: Double) = x = newX - width
    def bottomY_=(newY: Double) = y = newY - height
    
    def topLeft_=(p: Point) = position = p
    def topRight_=(p: Point) = position = p - X(width)
    def bottomRight_=(p: Point) = position = p - size
    def bottomLeft_=(p: Point) = position = p - Y(height)
    def center_=(p: Point) = position = p - size / 2
    
    
    // OTHER    ------------------------
    
    /**
      * Updates this component's position based on a mapping function result
      * @param f A mapping function for this component's position
      */
    def mapPosition(f: Point => Point) = position = f(position)
    /**
      * Updates this component's size based on a mapping function result
      * @param f A mapping function for this component's size
      */
    def mapSize(f: Size => Size) = size = f(size)
    /**
      * Updates this component's bounds based on a mapping function result
      * @param f A mapping function for this component's bounds
      */
    def mapBounds(f: Bounds => Bounds) = bounds = f(bounds)
    
    /**
     * The x or y coordinate of this component
     */
    @deprecated("Please use position.along(Axis) instead", "v2.0")
    def coordinateAlong(axis: Axis2D) = axis match {
        case X => x
        case Y => y
    }
    /**
      * @param axis Target axis
      * @return The right side x-coordinate or the bottom y-coordinate, depending on target axis
      */
    @deprecated("Please use maxAlong(Axis) instead", "v2.0")
    def maxCoordinateAlong(axis: Axis2D) = axis match
    {
        case X => rightX
        case Y => bottomY
    }
    
    /**
      * Updates one coordinate of this component
      * @param position New coordinate for this component
      */
    def setCoordinate(position: Vector1D) = this.position = this.position.withDimension(position)
    /**
     * Changes either x- or y-coordinate of this area
     * @param position the target coordinate
     * @param axis the target axis (X or Y)
     */
    @deprecated("Please use setCoordinate(Vector1D) instead", "v2.0")
    def setCoordinate(position: Double, axis: Axis2D) = axis match {
        case X => x = position
        case Y => y = position
    }
    
    /**
      * Translates this area's location
      * @param translation Translation to apply to this area's location
      */
    def translate(translation: HasDoubleDimensions) = position += translation
    /**
     * Adjusts either x- or y-coordinate of this area
     */
    @deprecated("Please rather use translate(Dimensional) or position += axis(adjustment)", "v2.0")
    def adjustCoordinate(adjustment: Double, axis: Axis2D) = axis match {
        case X => x += adjustment
        case Y => y += adjustment
    }
    
    /**
      * Updates either the width or height of this component
      * @param newLength New length for this component, including targeted axis
      */
    def setLength(newLength: Vector1D) = size = size.withLength(newLength)
    /**
     * Changes either the width or height of this area
     * @param length the new side length
     * @param axis the target axis (X for width, Y for height)
     */
    @deprecated("Please use setLength(Vector1D) instead", "v2.0")
    def setLength(length: Double, axis: Axis2D) = axis match {
        case X => width = length
        case Y => height = length
    }
    
    /**
     * Adjusts either the width (for X-axis) or height (for Y-axis) of this component
     */
    @deprecated("Please use size += axis(adjustment) instead", "v2.0")
    def adjustLength(adjustment: Double, axis: Axis2D) = axis match {
        case X => width += adjustment
        case Y => height += adjustment
    }
}
