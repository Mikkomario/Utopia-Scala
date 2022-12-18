package utopia.paradigm.shape.shape2d

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, NumericSpan}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.{Combinable, LinearScalable}
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis, Direction2D}
import utopia.paradigm.generic.ParadigmDataType.BoundsType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape1d.Span1D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{Dimensional, Dimensions, DimensionsWrapperFactory, DoubleVectorLike, HasDimensions}

import java.awt.geom.RoundRectangle2D
import scala.language.implicitConversions
import scala.util.Success

object Bounds extends DimensionsWrapperFactory[NumericSpan[Double], Bounds] with FromModelFactory[Bounds]
{
    // ATTRIBUTES    ----------------------
    
    override val zeroDimension = NumericSpan(0.0, 0.0)
    override protected val dimensionsFactory = Dimensions(zeroDimension)
    
    /**
     * A zero bounds
     */
    val zero = new Bounds(Right(dimensionsFactory.zero2D))
    
    /**
      * Collision axes used when testing for containment / overlap with Bounds
      */
    lazy val collisionAxes = Vector(X.unit.toVector2D, Y.unit.toVector2D)
    
    
    // IMPLICIT ---------------------------
    
    implicit def fromAwt(awtBounds: java.awt.Rectangle): Bounds =
        Bounds(Point(awtBounds.x, awtBounds.y), Size(awtBounds.width, awtBounds.height))
    
    
    // IMPLEMENTED  -----------------------
    
    override def apply(dimensions: Dimensions[NumericSpan[Double]]) =
        new Bounds(Right(dimensions.withLength(2)))
    
    override def from(other: HasDimensions[NumericSpan[Double]]) = other match {
        case b: Bounds => b
        case o => apply(o.dimensions)
    }
    
    override def apply(model: template.ModelLike[Property]) =
        Success(Bounds(model("position").getPoint, model("size").getSize))
    
    
    // OTHER    -----------------------
    
    /**
      * @param position The top-left corner of these bounds
      * @param size The size of these bounds
      * @return A set of bounds that combines these two values
      */
    def apply(position: Point, size: Size) = new Bounds(Left(position -> size))
    
    /**
      * Creates a new set of bounds
      * @param x Top-left x-coordinate
      * @param y Top-left y-coordinate
      * @param width Area width
      * @param height Area height
      * @return A new set of bounds
      */
    def apply(x: Double, y: Double, width: Double, height: Double): Bounds = apply(Point(x, y), Size(width, height))
    
    /**
     * Creates a rectangle that contains the area between the two coordinates. The order 
     * of the coordinates does not matter.
     */
    def between(p1: Point, p2: Point) =
        from(p1.dimensions.zipIteratorWith(p2.dimensions).map { case (s, e) => NumericSpan(s, e) })
    
    /**
     * Creates a set of bounds around a circle so that the whole sphere is contained.
     */
    def around(circle: Circle) = {
        val r = Vector3D(circle.radius, circle.radius, circle.radius)
        Bounds(circle.origin - r, (r * 2).toSize)
    }
    
    /**
     * Creates a set of bounds that contains all of the provided bounds. Returns none if the provided 
     * collection is empty.
     */
    def aroundOption(bounds: Iterable[Bounds]) = {
        if (bounds.isEmpty)
            None
        else if (bounds.size == 1)
            Some(bounds.head)
        else {
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
 * @since Genesis 13.1.2017
 */
class Bounds private(data: Either[(Point, Size), Dimensions[NumericSpan[Double]]])
    extends Dimensional[NumericSpan[Double], Bounds] with Rectangular with ValueConvertible with ModelConvertible
        with LinearScalable[Bounds] with Combinable[HasDoubleDimensions, Bounds] with Bounded[Bounds]
{
    // ATTRIBUTES   ----------------------
    
    override lazy val dimensions = data.rightOrMap { case (position, size) =>
        Bounds.dimensionsFactory.fromFunction2D { axis =>
            val start = position(axis)
            NumericSpan(start, start + size(axis))
        }
    }
    override lazy val position = data match {
        case Left((pos, _)) => pos
        case Right(dimensions) => Point(dimensions.map { _.start })
    }
    override lazy val size = data match {
        case Left((_, size)) => size
        case Right(dimensions) => Size(dimensions.map { _.length })
    }
    
    override lazy val components =
        dimensions.zipWithAxisIterator.map { case (span, axis) => Span1D(span, axis) }.toVector
    
    
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
      * @return A rounded version of these bounds
      */
    def round = mapEachDimension { _.mapEnds { _.round.toDouble } }
    /**
      * @return A copy of these bounds that rounds values for increased size and decreased position
      */
    def ceil = mapEachDimension { s => s.withEnds(s.start.floor, s.end.ceil) }
    /**
      * @return A copy of these bounds for decreased size and increased position
      */
    def floor = mapEachDimension { s => s.withEnds(s.start.ceil, s.end.floor) }
    
    
    // IMPLEMENTED METHODS    ----------
    
    override def self = this
    override def bounds = this
    
    override def topLeftCorner = position
    override def bottomRightCorner = position + size
    
    override def width = size.width
    override def height = size.height
    
    override def rightEdgeLength = height
    
    override def toValue = new Value(Some(this), BoundsType)
    override def toModel = Model(Vector("position" -> position, "size" -> size))
    override def toShape = toAwt
    
    override def topEdge = along(X).vector.in2D
    override def rightEdge = along(Y).vector.in2D
    
    override def collisionAxes = Bounds.collisionAxes
    
    override def withDimensions(newDimensions: Dimensions[NumericSpan[Double]]) = Bounds(newDimensions)
    override def withBounds(newBounds: Bounds) = newBounds
    
    override def translated(translation: HasDoubleDimensions): Bounds = withPosition(position + translation)
    
    /**
      * Scales both position and size
      * @param scaling A scaling factor
      * @return A scaled version of these bounds
      */
    override def *(scaling: Double) = mapEachDimension { _ * scaling }
    
    /**
      * @param translation Translation applied to these bounds
      * @return A translated set of bounds
      */
    override def +(translation: HasDoubleDimensions) = mergeWith(translation) { _ + _ }
    
    override def along(axis: Axis) = components.getOrElse(axis.index, Span1D.zeroAlong(axis))
    
    
    // OPERATORS    --------------------
    
    /**
      * @param insets Insets to add to these bounds
      * @return A copy of these bounds with specified insets added to the sides
      */
    // TODO: Refactor once insets have been refactored
    def +(insets: Insets) = Bounds(position - Vector2D(insets.top, insets.left), size + insets.total)
    
    /**
      * @param translation Translation applied to these bounds
      * @return A translated set of bounds
      */
    def -[V <: DoubleVectorLike[V]](translation: V) = translated(-translation)
    /**
      * @param insets Insets to subtract from these bounds
      * @return A copy of these bounds with the specified insets subtracted
      */
    // TODO: Refactor once insets have been refactored
    def -(insets: Insets) = Bounds(position + Vector2D(insets.top, insets.left), size - insets.total)
    
    /**
      * Scales both position and size
      * @param scaling A scaling factor
      * @return A scaled version of these bounds
      */
    def *(scaling: HasDoubleDimensions) = mergeWith(scaling) { _ * _ }
    /**
      * Divides both position and size
      * @param div A dividing factor
      * @return A divided version of these bounds
      */
    def /(div: HasDoubleDimensions) = mergeWith(div) { (span, div) => if (div == 0.0) span else span.mapEnds { _ / div } }
    
    
    // OTHER METHODS    ----------------
    
    /**
     * Creates a rounded rectangle based on this rectangle shape.
     * @param roundingFactor How much the corners are rounded. 0 Means that the corners are not
     * rounded at all, 1 means that the corners are rounded as much as possible, so that the ends of
     * the shape become ellipsoid. Default value is 0.5
     */
    def toRoundedRectangle(roundingFactor: Double = 0.5) = {
        val rounding = math.min(width, height) * roundingFactor
        new RoundRectangle2D.Double(position.x, position.y, width, height, rounding, rounding)
    }
    /**
      * Creates a rounded rectangle based on this rectangle shape
      * @param radius The radius to use when drawing the corners
      * @return A new rounded rectangle
      */
    def toRoundedRectangleWithRadius(radius: Double) =
        new RoundRectangle2D.Double(position.x, position.y, width, height, radius * 2, radius * 2)
    
    /**
     * Checks whether the line completely lies within the rectangle bounds
     */
    def contains(line: Line): Boolean = line.points.forall(contains)
    /**
     * Checks whether a set of bounds is contained within this bounds' area
     */
    def contains(bounds: Bounds): Boolean = forAllDimensionsWith(bounds) { _ contains _ }
    /**
      * @param item An item with bounds
      * @return Whether these bounds completely contain the specified item
      */
    def contains(item: HasBounds): Boolean = contains(item.bounds)
    /**
     * Checks whether a circle completely lies within the rectangle's bounds when the z-axis is 
     * ignored
     */
    def contains(circle: Circle): Boolean = contains(circle.origin) && circleIntersection(circle).isEmpty
    
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
      * @param p New position
      * @return A copy of these bounds with specified position
      */
    override def withPosition(p: Point): Bounds = Bounds(p, size)
    /**
      * @param map A mapping function for position
      * @return A copy of these bounds with mapped position
      */
    def mapPosition(map: Point => Point) = withPosition(map(position))
    
    /**
      * @param other Another area
      * @return The intersection between these two areas. None if there is no intersection.
      */
    @deprecated("Replaced with .overlapWith", "v1.2")
    def intersectionWith(other: Bounds) = overlapWith(other)
    /**
      * @param other Another area
      * @return The intersection between these two areas. None if there is no intersection.
      */
    def overlapWith(other: HasDimensions[HasInclusiveEnds[Double]]) = {
        x.overlapWith(other.x).flatMap { xOverlap =>
            y.overlapWith(other.y).map { yOverlap => Bounds(xOverlap, yOverlap) }
        }
    }
    /**
      * @param other Another set of bounds
      * @return The overlap between these two sets of bounds
      */
    def &&(other: Bounds) = overlapWith(other)
    
    /**
      * Fits these bounds to the specified area. Alters the size and position as little as possible.
      * Prefers to move position instead of changing size.
      * @param area Target area
      * @return These bounds fitted to the specified area.
      */
    // TODO: Rename and/or refactor
    def fittedInto(area: Bounds) =
    {
        // Case: Already fits
        if (area.contains(this))
            this
        // Case: Only position needs to be adjusted
        else if (size.fitsWithin(area.size))
        {
            val newPosition = Point.fromFunction2D { axis =>
                if (maxAlong(axis) > area.maxAlong(axis))
                    area.maxAlong(axis) - size(axis)
                else if (minAlong(axis) < area.minAlong(axis))
                    area.position(axis)
                else
                    position(axis)
            }
            withPosition(newPosition)
        }
        // Case: Only height (and possibly x) needs to be adjusted
        else if (width <= area.width)
        {
            val newX = if (rightX >= area.rightX) area.rightX - width else if (position.x <= area.position.x) area.position.x else position.x
            Bounds(Point(newX, area.position.y), Size(width, area.height))
        }
        // Case: Only width (and possibly y) needs to be adjusted
        else if (height <= area.height)
        {
            val newY = if (bottomY >= area.bottomY) area.bottomY - height else if (position.y <= area.position.y) area.position.y else position.y
            Bounds(Point(area.position.x, newY), Size(area.width, height))
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
    def bottomSlice(maxHeight: Double) = {
        if (height <= maxHeight)
            this
        else
            Bounds(position + Y(height - maxHeight), Size(width, maxHeight))
    }
    /**
      * @param maxWidth Maximum width of resulting bounds
      * @return A leftmost part of these bounds with up to a specific width
      */
    def leftSlice(maxWidth: Double) = if (width <= maxWidth) this else withWidth(maxWidth)
    /**
      * @param maxWidth Maximum width of resulting bounds
      * @return A rightmost part of these bounds with up to a specific width
      */
    def rightSlice(maxWidth: Double) = {
        if (width <= maxWidth)
            this
        else
            Bounds(position + X(width - maxWidth), Size(maxWidth, height))
    }
    /**
      * Slices these bounds from a specific direction
      * @param direction The direction from which these bounds are sliced
      * @param maxLength The maximum length of the taken are (parallel to 'direction')
      * @return A slice of these bounds
      */
    def slice(direction: Direction2D, maxLength: Double) = direction match {
        case Direction2D.Up => topSlice(maxLength)
        case Direction2D.Down => bottomSlice(maxLength)
        case Direction2D.Left => leftSlice(maxLength)
        case Direction2D.Right => rightSlice(maxLength)
    }
}