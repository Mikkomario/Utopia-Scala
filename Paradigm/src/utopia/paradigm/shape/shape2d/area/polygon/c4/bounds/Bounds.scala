package utopia.paradigm.shape.shape2d.area.polygon.c4.bounds

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.{HasInclusiveOrderedEnds, NumericSpan}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.generic.model.template
import utopia.flow.generic.model.template.{ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.combine.{Combinable, LinearScalable, Subtractable}
import utopia.flow.operator.equality.EqualsBy
import utopia.flow.util.NotEmpty
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis, Direction2D}
import utopia.paradigm.generic.ParadigmDataType.BoundsType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.shape.shape1d.range.Span1D
import utopia.paradigm.shape.shape1d.vector.Vector1D
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.Rectangular
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.NumericVectorFactory
import utopia.paradigm.shape.template.{Dimensional, Dimensions, HasDimensions}

import java.awt.geom.RoundRectangle2D
import scala.language.implicitConversions
import scala.math.Numeric.DoubleIsFractional
import scala.util.Success

object Bounds extends BoundsFactoryLike[Double, Point, Size, Bounds] with FromModelFactory[Bounds]
{
    // ATTRIBUTES    ----------------------
    
    override val zeroDimension = super.zeroDimension
    override protected val dimensionsFactory = super.dimensionsFactory
    override val zero = super.zero
    
    /**
      * Collision axes used when testing for containment / overlap with Bounds
      */
    lazy val collisionAxes = Vector(X.unit.toVector2D, Y.unit.toVector2D)
    
    
    // IMPLICIT ---------------------------
    
    implicit def fromAwt(awtBounds: java.awt.Rectangle): Bounds = apply(awtBounds)
    
    
    // IMPLEMENTED  -----------------------
    
    override implicit def n: Fractional[Double] = DoubleIsFractional
    override protected def pointFactory: NumericVectorFactory[Double, Point] = Point
    override protected def sizeFactory: NumericVectorFactory[Double, Size] = Size
    
    override def apply(dimensions: Dimensions[NumericSpan[Double]]) =
        new Bounds(dimensions.withLength(2))
    
    override def from(other: HasDimensions[NumericSpan[Double]]) = other match {
        case b: Bounds => b
        case o => apply(o.dimensions)
    }
    
    override def apply(model: template.ModelLike[Property]) =
        Success(Bounds(model("position").getPoint, model("size").getSize))
    
    
    // OTHER    -----------------------
    
    /**
      * Creates a set of bounds around a circle
      */
    def around(circle: Circle) = fromFunction2D { axis =>
        val o = circle.origin(axis)
        NumericSpan(o - circle.radius, o + circle.radius)
    }
    
    /**
     * Creates a set of bounds that contains all of the provided bounds. Returns none if the provided 
     * collection is empty.
     */
    def aroundOption(bounds: Iterable[Bounds]) = NotEmpty(bounds).map(around)
    /**
     * Creates a bounds instance that contains all specified bounds. Will throw on empty collection
     */
    def around(bounds: Iterable[Bounds]) = {
        if (bounds hasSize 1)
            bounds.head
        else {
            val topLeft = Point.topLeft(bounds.map { _.topLeft })
            val bottomRight = Point.bottomRight(bounds.map { _.bottomRight })
            between(topLeft, bottomRight)
        }
    }
    
    /**
     * Creates a rectangle around line so that the line becomes one of the rectangle's diagonals
     */
    def aroundDiagonal(diagonal: Line) = between(diagonal.start, diagonal.end)
}

/**
 * Bounds limit a certain rectangular area of space. The rectangle is defined by two points 
 * and the edges go along x and y axes.
 * @author Mikko Hilpinen
 * @since Genesis 13.1.2017
 */
class Bounds private(override val dimensions: Dimensions[NumericSpan[Double]])
    extends Dimensional[NumericSpan[Double], Bounds] with Rectangular with ValueConvertible with ModelConvertible
        with LinearScalable[Bounds] with Combinable[HasDoubleDimensions, Bounds]
        with Subtractable[HasDoubleDimensions, Bounds] with Bounded[Bounds] with EqualsBy
{
    // ATTRIBUTES   ----------------------
    
    // NB: position + size seems to break in a lot of places =>
    // Will need to be careful with these, in case they break the compiler again in the future
    override lazy val position: Point = Point(dimensions.map { _.min })
    override lazy val size: Size = Size(dimensions.map { _.length.abs })
    
    override lazy val components: Vector[Span1D] =
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
    def ceil = mapEachDimension { s =>
        val minMax = s.minMax
        s.withEnds(minMax.first.floor, minMax.second.ceil)
    }
    /**
      * @return A copy of these bounds for decreased size and increased position
      */
    def floor = mapEachDimension { s =>
        val minMax = s.minMax
        s.withEnds(minMax.first.ceil, minMax.second.floor)
    }
    
    
    // IMPLEMENTED METHODS    ----------
    
    override def self = this
    override def bounds = this
    
    override protected def equalsProperties = Vector(dimensions)
    
    override def topLeftCorner = Point(dimensions.map { _.start })
    override def bottomRightCorner: Point = Point(dimensions.map { _.end })
    
    override def width = size.width
    override def height = size.height
    
    override def rightEdgeLength = height
    
    override def toValue = new Value(Some(this), BoundsType)
    override def toModel = Model.from("position" -> (position: Value), "size" -> (size: Value))
    override def toShape = toAwt
    
    override def topEdge = along(X).vector.in2D
    override def rightEdge = along(Y).vector.in2D
    
    override def center = Point(dimensions.map { s => (s.start + s.end) / 2 })
    
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
    override def -(other: HasDoubleDimensions): Bounds = mergeWith(other) { _ - _ }
    
    override def along(axis: Axis) = components.getOrElse(axis.index, Span1D.zeroAlong(axis))
    
    
    // OTHER    --------------------
    
    /**
      * @param insets Insets to add to these bounds
      * @return A copy of these bounds with specified insets added to the sides
      */
    def +(insets: Insets) = mergeWith(insets) { (area, insets) =>
        val minMax = area.minMax
        area.withEnds(minMax.first - insets.first, minMax.second + insets.second)
    }
    /**
      * @param insets Insets to subtract from these bounds
      * @return A copy of these bounds with the specified insets subtracted
      */
    def -(insets: Insets) = mergeWith(insets) { (area, insets) =>
        val minMax = area.minMax
        area.withEnds(minMax.first + insets.first, minMax.second - insets.second)
    }
    
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
    
    /**
     * Checks whether the line completely lies within the rectangle bounds
     */
    def contains(line: Line): Boolean = line.ends.forall(contains)
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
      * Converts an absolute point to a coordinate that is relative to this set of bounds.
      * In the relative (i.e. resulting) coordinate system,
      *     (0,0) represents the top-left corner of these bounds
      *     and (1,1) represents the bottom-right corner of these bounds.
      * The resulting point may be outside of this range, if it doesn't lie within these bounds
      * @param point A point to convert into the relative coordinate system
      * @return A relative representation of the specified point
      */
    def relativize(point: Point) =
        point.mergeWith(this) { (coordinate, area) => (coordinate - area.start) / area.length }
    /**
      * Converts an absolute set of bounds to a set of bounds that is relative to this set of bounds.
      * In the relative (i.e. resulting) coordinate system,
      * (0,0) represents the top-left corner of these bounds
      * and (1,1) represents the bottom-right corner of these bounds.
      * The resulting set of bounds may fully or partially lay outside of this range,
      * if it doesn't lie within these bounds
      * @param area A set of bounds to convert into the relative coordinate system
      * @return A relative representation of the specified bounds
      */
    def relativize(area: Bounds) =
        area.mergeWith(this) { (targetArea, myArea) =>
            val myLength = myArea.length
            targetArea.mapEnds { coordinate => (coordinate - myArea.start) / myLength }
        }
    /**
      * Converts an absolute line to a line that is relative to this set of bounds.
      * In the relative (i.e. resulting) coordinate system,
      * (0,0) represents the top-left corner of these bounds
      * and (1,1) represents the bottom-right corner of these bounds.
      * The resulting line may fully or partially lie outside of this range, if it doesn't lie within these bounds
      * @param line A line to convert into the relative coordinate system
      * @return A relative representation of the specified line
      */
    def relativize(line: Line): Line = line.map(relativize)
    
    /**
      * Converts a point in the relative coordinate system to a point in the absolute coordinate system.
      * In the relative (i.e. resulting) coordinate system,
      *     (0,0) represents the top-left corner of these bounds
      *     and (1,1) represents the bottom-right corner of these bounds.
      * @param relativePoint A point that is relative to this set of bounds
      * @return An absolute representation of the specified point
      */
    def relativeToAbsolute(relativePoint: Point) = relativePoint.mergeWith(this) { (relative, area) =>
        area.start + relative * area.length
    }
    /**
      * Converts a set of bounds in the relative coordinate system to a set of bounds in the absolute coordinate system.
      * In the relative (i.e. resulting) coordinate system,
      * (0,0) represents the top-left corner of these bounds
      * and (1,1) represents the bottom-right corner of these bounds.
      * @param relativeArea A set of bounds that is relative to this set of bounds
      * @return An absolute representation of the bounds
      */
    def relativeToAbsolute(relativeArea: Bounds) = relativeArea.mergeWith(this) { (relativeArea, area) =>
        val myLength = area.length
        relativeArea.mapEnds { relative => area.start + relative * myLength }
    }
    /**
      * Converts a line in the relative coordinate system to a line in the absolute coordinate system.
      * In the relative (i.e. resulting) coordinate system,
      * (0,0) represents the top-left corner of these bounds
      * and (1,1) represents the bottom-right corner of these bounds.
      * @param relativeLine A point that is relative to this set of bounds
      * @return An absolute representation of the specified line
      */
    def relativeToAbsolute(relativeLine: Line): Line = relativeLine.map(relativeToAbsolute)
    
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
      * @param other Another area
      * @return The intersection between these two areas. None if there is no intersection.
      */
    @deprecated("Replaced with .overlapWith", "v1.2")
    def intersectionWith(other: Bounds) = overlapWith(other)
    /**
      * @param other Another area
      * @return The intersection between these two areas. None if there is no intersection.
      */
    def overlapWith(other: HasDimensions[HasInclusiveOrderedEnds[Double]]) = {
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
      * Creates a rounded rectangle based on this rectangle shape.
      * @param roundingFactor How much the corners are rounded. 0 Means that the corners are not
      *                       rounded at all, 1 means that the corners are rounded as much as possible, so that the ends of
      *                       the shape become ellipsoid. Default value is 0.5
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
        else {
            val adjust = Y(height - maxHeight)
            Bounds(position + adjust, Size(width, maxHeight))
        }
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
        else {
            val adjust: Vector1D = X(width - maxWidth)
            Bounds(position + adjust, Size(maxWidth, height))
        }
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