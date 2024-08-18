package utopia.paradigm.shape.shape2d.area.polygon.c4.bounds

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.NumericSpan
import utopia.paradigm.shape.template.{DimensionalFactory, DimensionsWrapperFactory, HasDimensions}

/**
  * Common trait for factory classes used for building bounds-like instances
  * (i.e. rectangular areas aligned with X and Y -axes)
  * @author Mikko Hilpinen
  * @since 27.8.2023
  * @tparam D Type of dimensions used in position and size
  * @tparam P Type of points used for position information
  * @tparam S Type of sizes used
  * @tparam B Type of resulting bounds
  */
trait BoundsFactoryLike[D, P <: HasDimensions[D], S <: HasDimensions[D], +B]
    extends DimensionsWrapperFactory[NumericSpan[D], B]
{
    // ABSTRACT ---------------------------
    
    implicit def n: Fractional[D]
    
    protected def pointFactory: DimensionalFactory[D, P]
    protected def sizeFactory: DimensionalFactory[D, S]
    
    
    // COMPUTED ---------------------------
    
    /**
      * A zero bounds (i.e. bounds of size zero, located at the origin (0,0))
      */
    def zero = apply(dimensionsFactory.zero2D)
    
    
    // IMPLEMENTED  -----------------------
    
    override def zeroDimension = NumericSpan.singleValue(n.zero)
    
    
    // OTHER    -----------------------
    
    /**
      * @param position The top-left corner of these bounds
      * @param size The size of these bounds
      * @return A set of bounds that combines these two values
      */
    def apply(position: P, size: S): B =
        apply(position.dimensions.mergeWith(size.dimensions, zeroDimension) { (s, len) =>
            NumericSpan(s, n.plus(s, len))
        })
    
    /**
      * Creates a new set of bounds
      * @param x Top-left x-coordinate
      * @param y Top-left y-coordinate
      * @param width Area width
      * @param height Area height
      * @return A new set of bounds
      */
    def apply(x: D, y: D, width: D, height: D): B =
        apply(NumericSpan(x, n.plus(x, width)), NumericSpan(y, n.plus(y, height)))
    
    /**
     * Creates a rectangle that contains the area between the two coordinates.
      * The order of the coordinates does not matter.
     */
    def between(p1: HasDimensions[D], p2: HasDimensions[D]) =
        from(p1.dimensions.zipIteratorWith(p2.dimensions).map { case (s, e) => NumericSpan(s, e) })
    /**
     * @param points Two points
     * @return A set of bounds that just contains the two specified points
     */
    def between(points: Pair[HasDimensions[D]]): B = between(points.first, points.second)
    
    /**
      * Creates a set of bounds centered around a specific point
      * @param center The center point of these bounds
      * @param size   The size of these bounds
      * @return A new set of bounds
      */
    def centered(center: P, size: S) = fromFunction2D { axis =>
        val c = center(axis)
        val r = n.div(size(axis), n.fromInt(2))
        NumericSpan(n.minus(c, r), n.plus(c, r))
    }
    
    /**
      * Converts AWT bounds to this type of bounds
      * @param awtBounds A set of bounds from AWT
      * @return A set of bounds based on the specified bounds
      */
    def apply(awtBounds: java.awt.Rectangle): B =
        apply(pointFactory(n.fromInt(awtBounds.x), n.fromInt(awtBounds.y)),
            sizeFactory(n.fromInt(awtBounds.width), n.fromInt(awtBounds.height)))
    
    /*
    /**
     * Creates a set of bounds that contains all of the provided bounds.
      * Returns none if the specified collection is empty.
     */
    // TODO: Change accepted type to HasBoundsLike (once available)
    def aroundOption(bounds: Iterable[HasDimensions[HasInclusiveEnds[D]]]) = {
        if (bounds.isEmpty)
            None
        else if (bounds hasSize 1)
            Some(bounds.head)
        else {
            val topLeft = pointFactory.topLeft(bounds.map { _.topLeft })
            val bottomRight = Point.bottomRight(bounds.map { _.bottomRight })
            between(topLeft, bottomRight)
        }
        NotEmpty(bounds).map(around)
    }
    /**
     * Creates a bounds instance that contains all specified bounds. Will throw on empty collection
     */
    def around(bounds: Iterable[Bounds]) = aroundOption.getOrElse(zero)
    */
}

