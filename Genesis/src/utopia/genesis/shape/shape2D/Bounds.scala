package utopia.genesis.shape.shape2D

import scala.language.implicitConversions

import java.awt.geom.RoundRectangle2D

import utopia.flow.generic.ValueConvertible
import utopia.flow.datastructure.immutable.Value
import utopia.genesis.generic.BoundsType
import utopia.flow.generic.ModelConvertible
import utopia.flow.datastructure.immutable.Model
import utopia.flow.generic.ValueConversions._
import utopia.flow.datastructure.template
import utopia.flow.generic.FromModelFactory
import utopia.flow.datastructure.template.Property
import utopia.genesis.generic.GenesisValue._
import utopia.genesis.shape.Axis._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape3D.Vector3D
import utopia.genesis.shape.template.VectorLike

import scala.util.Success

object Bounds extends FromModelFactory[Bounds]
{
    // ATTRIBUTES    ----------------------
    
    /**
     * A zero bounds
     */
    val zero = Bounds(Point.origin, Size.zero)
    
    
    // IMPLICIT ---------------------------
    
    implicit def fromAwt(awtBounds: java.awt.Rectangle): Bounds = Bounds(Point(awtBounds.x, awtBounds.y),
        Size(awtBounds.width, awtBounds.height))
    
    
    // OPERATORS    -----------------------
    
    override def apply(model: template.Model[Property]) =
        Success(Bounds(model("position").getPoint, model("size").getSize))
    
    
    // OTHER METHODS    -------------------
    
    /**
     * Creates a rectangle that contains the area between the two coordinates. The order 
     * of the coordinates does not matter.
     */
    def between(p1: Point, p2: Point) = 
    {
        val topLeft = p1 topLeft p2
        val bottomRight = p1 bottomRight p2
        
        Bounds(topLeft, (bottomRight - topLeft).toSize)
    }
    
    /**
     * Creates a set of bounds around a circle so that the whole sphere is contained.
     */
    def around(circle: Circle) = 
    {
        val r = Vector3D(circle.radius, circle.radius, circle.radius)
        Bounds(circle.origin - r, (r * 2).toSize)
    }
    
    /**
     * Creates a set of bounds that contains all of the provided bounds. Returns none if the provided 
     * collection is empty.
     */
    def aroundOption(bounds: Iterable[Bounds]) =
    {
        if (bounds.isEmpty)
            None
        else if (bounds.size == 1)
            Some(bounds.head)
        else
        {
            val topLeft = Point.topLeft(bounds.map{ _.topLeft })
            val bottomRight = Point.bottomRight(bounds.map { _.bottomRight })
            
            Some(between(topLeft, bottomRight))
        }
    }
    
    /**
     * Creates a bounds instance that contains all specified bounds. Will throw on empty collection
     */
    def around(bounds: Iterable[Bounds]) = aroundOption(bounds).get
    
    /**
     * Creates a rectangle around line so that the line becomes one of the rectangle's diagonals
     */
    def aroundDiagonal(diagonal: Line) = between(diagonal.start, diagonal.end)
    
    /**
      * Creates a set of bounds centered around a specific point
      * @param center The center point of these bounds
      * @param size The size of these bounds
      * @return A new set of bounds
      */
    def centered(center: Point, size: Size) = Bounds(center - size / 2, size)
}

/**
 * Bounds limit a certain rectangular area of space. The rectangle is defined by two points 
 * and the edges go along x and y axes.
 * @author Mikko Hilpinen
 * @since 13.1.2017
 */
case class Bounds(position: Point, size: Size) extends Rectangular with ValueConvertible with ModelConvertible
{
    // COMPUTED PROPERTIES    ------------
    
    /**
     * An awt counterpart of these bounds
     */
    def toAwt = new java.awt.Rectangle(position.x.round.toInt, position.y.round.toInt, width.round.toInt, height.round.toInt)
    
    /**
     * The diagonal line for this rectangle. Starts at the position coordinates and goes all the 
     * way to the opposite corner.
     */
    def diagonal = Line(topLeft, bottomRight)
    
    /**
      * @return The x-coordinate of these bounds
      */
    def x = position.x
    /**
      * @return The y-coordinate of these bounds
      */
    def y = position.y
    /**
      * @return The right side x-coordinate
      */
    def rightX = x + width
    /**
      * @return The bottom side y-coordinate
      */
    def bottomY = y + height
    
    /**
      * @return A rounded version of these bounds
      */
    def round =
    {
        val newPosition = position.round
        if (newPosition == position)
            Bounds(newPosition, size.round)
        else
            Bounds(newPosition, Size((rightX - newPosition.x).round.toDouble, (bottomY - newPosition.y).round.toDouble))
    }
    
    /**
      * @return A copy of these bounds that rounds values for increased size and decreased position
      */
    def ceil =
    {
        val newPosition = position.floor
        if (newPosition == position)
            Bounds(newPosition, size.ceil)
        else
            Bounds(newPosition, Size((rightX - newPosition.x).ceil, (bottomY - newPosition.y).ceil))
    }
    
    /**
      * @return A copy of these bounds for decreased size and increased position
      */
    def floor =
    {
        val newPosition = position.ceil
        if (newPosition == position)
            Bounds(newPosition, size.floor)
        else
            Bounds(newPosition, Size((rightX - newPosition.x).floor, (bottomY - newPosition.y).floor))
    }
    
    
    // IMPLEMENTED METHODS    ----------
    
    def topLeft = position
    
    override def bounds = this
    
    override def width = size.width
    
    override def height = size.height
    
    override def toValue = new Value(Some(this), BoundsType)
    
    override def toModel = Model(Vector("position" -> position, "size" -> size))
    
    override def toShape = toAwt
    
    override def leftLength = size.height
    
    override def top = X(size.width)
    
    override def bottomRight = position + size
    
    override def contains[V <: Vector2DLike[V]](point: V) = point.x >= topLeft.x && point.y >= topLeft.y &&
            point.x <= bottomRight.x && point.y <= bottomRight.y
    
    override def translated(translation: Vector2DLike[_]) = withPosition(position + translation)
    
    
    // OPERATORS    --------------------
    
    /**
      * @param translation Translation applied to these bounds
      * @return A translated set of bounds
      */
    def +(translation: Vector2DLike[_]) = translated(translation)
    
    /**
      * @param insets Insets to add to these bounds
      * @return A copy of these bounds with specified insets added to the sides
      */
    def +(insets: Insets) = Bounds(position - Vector2D(insets.top, insets.left), size + insets.total)
    
    /**
      * @param translation Translation applied to these bounds
      * @return A translated set of bounds
      */
    def -[V <: Vector2DLike[V]](translation: V) = translated(-translation)
    
    /**
      * @param insets Insets to subtract from these bounds
      * @return A copy of these bounds with the specified insets subtracted
      */
    def -(insets: Insets) = Bounds(position + Vector2D(insets.top, insets.left), size - insets.total)
    
    /**
      * Scales both position and size
      * @param scaling A scaling factor
      * @return A scaled version of these bounds
      */
    def *(scaling: VectorLike[_]) = Bounds(position * scaling, size * scaling)
    
    /**
      * Scales both position and size
      * @param scaling A scaling factor
      * @return A scaled version of these bounds
      */
    def *(scaling: Double) = Bounds(position * scaling, size * scaling)
    
    /**
      * Divides both position and size
      * @param div A dividing factor
      * @return A divided version of these bounds
      */
    def /(div: VectorLike[_]) = Bounds(position / div, size / div)
    
    /**
      * Divides both position and size
      * @param div A dividing factor
      * @return A divided version of these bounds
      */
    def /(div: Double) = Bounds(position / div, size / div)
    
    
    // OTHER METHODS    ----------------
    
    /**
      * Finds the minimum coordinate along specified axis (assuming positive size of these bounds)
      * @param axis Targeted axis
      * @return The coordinate of the top-left corner of these bounds along the specified axis
      */
    def minAlong(axis: Axis2D) = position.along(axis)
    /**
      * Finds the maximum coordinate along specified axis (assuming positive size of these bounds)
      * @param axis Targeted axis
      * @return The coordinate of the bottom-right corner of these bounds along the specified axis
      */
    def maxAlong(axis: Axis2D) = position.along(axis) + size.along(axis)
    
    /**
     * Creates a rounded rectangle based on this rectangle shape.
     * @param roundingFactor How much the corners are rounded. 0 Means that the corners are not
     * rounded at all, 1 means that the corners are rounded as much as possible, so that the ends of
     * the shape become ellipsoid. Default value is 0.5
     */
    def toRoundedRectangle(roundingFactor: Double = 0.5) =
    {
        val rounding = math.min(width, height) * roundingFactor
        new RoundRectangle2D.Double(position.x, position.y, width, height, rounding, rounding)
    }
    /**
      * Creates a rounded rectangle based on this rectangle shape
      * @param radius The radius to use when drawing the corners
      * @return A new rounded rectangle
      */
    def toRoundedRectangleWithRadius(radius: Double) =
    {
        new RoundRectangle2D.Double(position.x, position.y, width, height, radius * 2, radius * 2)
    }
    
    /**
     * Checks whether the line completely lies within the rectangle bounds
     */
    def contains(line: Line): Boolean = contains(line.start) && contains(line.end)
    /**
     * Checks whether a set of bounds is contained within this bounds' area
     */
    def contains(bounds: Bounds): Boolean = contains(bounds.topLeft) && contains(bounds.bottomRight)
    /**
     * Checks whether a circle completely lies within the rectangle's bounds when the z-axis is 
     * ignored
     */
    def contains(circle: Circle): Boolean = contains(circle.origin) && circleIntersection(circle).isEmpty
    
    /**
      * @param bounds Another set of bounds
      * @return Whether these bounds overlap with the other set of bounds
      */
    def overlapsWith(bounds: Bounds) = Axis2D.values.forall { axis =>
        maxAlong(axis) > bounds.minAlong(axis) && bounds.maxAlong(axis) > minAlong(axis) }
    
    /**
     * Finds the intersection points between the edges of this rectangle and the provided circle
     */
    def circleIntersection(circle: Circle) = sides.flatMap { _.circleIntersection(circle) }
    /**
     * Finds the intersection points between the edges of this rectangle and the provided line. 
     * Both shapes are projected to the x-y plane before the check.
     */
    def lineIntersection(line: Line) = sides.flatMap { _.intersection(line) }
    
    /**
      * Enlarges these bounds from the center
      * @param enlargement The size enlargement
      * @return A copy of these bounds with same center but increased size
      */
    def enlarged(enlargement: Size) = Bounds(position - enlargement / 2, size + enlargement)
    /**
      * Enlarges these bounds from the center
      * @param widthIncrease The increase in width
      * @param heightIncrease The increase in height
      * @return A copy of these bounds with same center but increased size
      */
    def enlarged(widthIncrease: Double, heightIncrease: Double): Bounds = enlarged(Size(widthIncrease, heightIncrease))
    
    /**
      * Shrinks these bounds from the center
      * @param shrinking The size decrease
      * @return A copy of these bounds with same center but decreased size
      */
    def shrinked(shrinking: Size) = enlarged(-shrinking)
    /**
      * Shrinks these bounds from the center
      * @param widthDecrease The decrease in width
      * @param heightDecrease The decrease in height
      * @return A copy of these bounds with same center but decreased size
      */
    def shrinked(widthDecrease: Double, heightDecrease: Double): Bounds = shrinked(Size(widthDecrease, heightDecrease))
    
    /**
      * @param p New position
      * @return A copy of these bounds with specified position
      */
    def withPosition(p: Point) = Bounds(p, size)
    /**
      * @param s New size
      * @return A copy of these bounds with specified size
      */
    def withSize(s: Size) = Bounds(position, s)
    /**
      * @param w New width
      * @return A copy of these bounds with specified width
      */
    def withWidth(w: Double) = withSize(Size(w, height))
    /**
      * @param h New height
      * @return A copy of these bounds with specified height
      */
    def withHeight(h: Double) = withSize(Size(width, h))
    /**
      * @param map A mapping function for position
      * @return A copy of these bounds with mapped position
      */
    def mapPosition(map: Point => Point) = withPosition(map(position))
    
    /**
      * @param map A mapping function for size
      * @return A copy of these bounds with mapped size
      */
    def mapSize(map: Size => Size) = withSize(map(size))
    
    /**
      * @param x X-translation applied
      * @param y Y-translation applied
      * @return A copy of these bounds with translated position
      */
    def translatedBy(x: Double, y: Double) = withPosition(position + Vector2D(x, y))
    
    /**
      * @param area Another area
      * @return The intersection between these two areas. None if there is no intersection.
      */
    def within(area: Bounds) =
    {
        val newTopLeft = topLeft.bottomRight(area.topLeft)
        val newBottomRight = bottomRight.topLeft(area.bottomRight)
        
        if (newTopLeft.x > newBottomRight.x || newTopLeft.y > newBottomRight.y)
            None
        else
            Some(Bounds.between(newTopLeft, newBottomRight))
    }
    
    /**
      * Fits these bounds to the specified area. Alters the size and position as little as possible.
      * Prefers to move position instead of changing size.
      * @param area Target area
      * @return These bounds fitted to the specified area.
      */
    def fittedInto(area: Bounds) =
    {
        // Case: Already fits
        if (area.contains(this))
            this
        // Case: Only position needs to be adjusted
        else if (size.fitsInto(area.size))
        {
            val newPosition = Point.calculateWith { axis =>
                if (maxAlong(axis) > area.maxAlong(axis))
                    area.maxAlong(axis) - size.along(axis)
                else if (minAlong(axis) < area.minAlong(axis))
                    area.position.along(axis)
                else
                    position.along(axis)
            }
            withPosition(newPosition)
        }
        // Case: Only height (and possibly x) needs to be adjusted
        else if (width <= area.width)
        {
            val newX = if (rightX >= area.rightX) area.rightX - width else if (x <= area.x) area.x else x
            Bounds(Point(newX, area.y), Size(width, area.height))
        }
        // Case: Only width (and possibly y) needs to be adjusted
        else if (height <= area.height)
        {
            val newY = if (bottomY >= area.bottomY) area.bottomY - height else if (y <= area.y) area.y else y
            Bounds(Point(area.x, newY), Size(area.width, height))
        }
        // Case: Both height and width need to be shrunk
        else
            area
    }
    
    /**
      * @param maxHeight Maximum height of resulting bounds
      * @return A top part of these bounds with up to a specific height
      */
    def topSlice(maxHeight: Double) = if (height <= maxHeight) this else withHeight(maxHeight)
    /**
      * @param maxHeight Maximum height of resulting bounds
      * @return A bottom part of these bounds with up to a specific height
      */
    def bottomSlice(maxHeight: Double) = if (height <= maxHeight) this else
        Bounds(position.plusY(height - maxHeight), Size(width, maxHeight))
    /**
      * @param maxWidth Maximum width of resulting bounds
      * @return A leftmost part of these bounds with up to a specific width
      */
    def leftSlice(maxWidth: Double) = if (width <= maxWidth) this else withWidth(maxWidth)
    /**
      * @param maxWidth Maximum width of resulting bounds
      * @return A rightmost part of these bounds with up to a specific width
      */
    def rightSlice(maxWidth: Double) = if (width <= maxWidth) this else
        Bounds(position.plusX(width - maxWidth), Size(maxWidth, height))
    /**
      * Slices these bounds from a specific direction
      * @param direction The direction from which these bounds are sliced
      * @param maxLength The maximum length of the taken are (parallel to 'direction')
      * @return A slice of these bounds
      */
    def slice(direction: Direction2D, maxLength: Double) = direction match
    {
        case Direction2D.Up => topSlice(maxLength)
        case Direction2D.Down => bottomSlice(maxLength)
        case Direction2D.Left => leftSlice(maxLength)
        case Direction2D.Right => rightSlice(maxLength)
    }
}