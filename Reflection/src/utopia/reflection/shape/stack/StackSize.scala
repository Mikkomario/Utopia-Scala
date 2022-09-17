package utopia.reflection.shape.stack

import utopia.flow.datastructure.immutable.Pair
import utopia.flow.operator.{Combinable, LinearScalable}
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.{Insets, MultiDimensional, Size, TwoDimensional}
import utopia.reflection.shape.stack.LengthPriority.Low

object StackSize
{
    // ATTRIBUTES    --------------------
    
    /**
     * A stacksize that allows any value while preferring a zero size
     */
    val any: StackSize = any(Size.zero)
    /**
     * A stack size that is always 0x0
     */
    val fixedZero = fixed(Size.zero)
    
    
    // CONSTRUCTOR    -------------------
    
    /**
      * @param width Width of this size
      * @param height Height of this size
      * @return A new size
      */
    def apply(width: StackLength, height: StackLength): StackSize = apply(Pair(width, height))
    /**
      * @param min Minimum size
      * @param optimal Optimal size
      * @param maxWidth Maximum width (None if not limited)
      * @param maxHeight Maximum height (None if not limited)
      * @return A new stack size
      */
    def apply(min: Size, optimal: Size, maxWidth: Option[Double], maxHeight: Option[Double]) =
            new StackSize(StackLength(min.width, optimal.width, maxWidth),
            StackLength(min.height, optimal.height, maxHeight))
    /**
      * @param min Minimum size
      * @param optimal Optimal size
      * @param max Maximum size (None if not limited)
      * @return A new stack size
      */
    def apply(min: Size, optimal: Size, max: Option[Size]): StackSize = apply(min, optimal, 
            max.map{ _.width }, max.map{ _.height })
    /**
      * @param min Minimum size
      * @param optimal Optimal size
      * @param max Maximum size
      * @return A new stack size
      */
    def apply(min: Size, optimal: Size, max: Size): StackSize = apply(min, optimal, Some(max))
    /**
      * @param length Parallel stack length
      * @param breadth Perpendicular stack length
      * @param axis Axis that determines what is parallel and what is perpendicular
      * @return A new stack size
      */
    def apply(length: StackLength, breadth: StackLength, axis: Axis2D): StackSize = axis match 
    {
        case X => StackSize(length, breadth)
        case Y => StackSize(breadth, length)
    }
    
    /**
      * @param optimal Optimal size
      * @return A new stack size with no mimimum or maximum
      */
    def any(optimal: Size) = StackSize(Size.zero, optimal, None)
    /**
      * @param size Fixed size
      * @return A Stack size that is fixed to specified size
      */
    def fixed(size: Size) = StackSize(size, size, size)
    /**
      * @param min Minimum size
      * @param optimal Optimal size
      * @return A stack size that has no maximum
      */
    def upscaling(min: Size, optimal: Size) = StackSize(min, optimal, None)
    /**
      * @param optimal Optimal size
      * @return A stack size that has no maximum. Optimal is used as minimum.
      */
    def upscaling(optimal: Size): StackSize = upscaling(optimal, optimal)
    /**
      * @param optimal Optimal size
      * @param maxWidth Maximum width
      * @param maxHeight Maximum height
      * @return A stack size with no mimimum
      */
    def downscaling(optimal: Size, maxWidth: Double, maxHeight: Double) = StackSize(Size.zero, optimal,
            Some(maxWidth), Some(maxHeight))
    /**
      * @param optimal Optimal size
      * @param max Maximum size
      * @return A stack size with no minimum
      */
    def downscaling(optimal: Size, max: Size): StackSize = StackSize(Size.zero, optimal, max)
    /**
      * @param max Maximum and optimal size
      * @return A stack size with no minimum. Max is used as optimal.
      */
    def downscaling(max: Size): StackSize = downscaling(max, max)
    
    /**
      * @param side A function for building a stack length
      * @return A stack size with builder function results
      */
    def build(side: Axis2D => StackLength) = apply(side(X), side(Y))
    
    /**
      * @param sizes Sizes to combine
      * @return A size that best suits all of the requirements
      */
    def combine(sizes: Seq[StackSize]) = build { axis => StackLength.combine(sizes.map { _.along(axis) }) }
}

/**
* This class represents a size that may vary within minimum and maximum limits
* @author Mikko Hilpinen
* @since 25.2.2019
**/
case class StackSize(override val dimensions2D: Pair[StackLength])
    extends TwoDimensional[StackLength] with StackInsetsConvertible with LinearScalable[StackSize]
        with Combinable[StackSize, StackSize]
{
    // COMPUTED PROPERTIES    --------
    
    /**
      * @return Width of this size
      */
    def width = dimensions2D.first
    /**
      * @return Height of this size
      */
    def height = dimensions2D.second
    
    /**
      * @return Minimum size
      */
    def min = Size(dimensions2D.map { _.min })
    /**
      * @return Optimum size
      */
    def optimal = Size(dimensions2D.map { _.optimal })
    /**
      * @return Maximum size. None if not specified
      */
    def max = 
    {
        if (dimensions2D.forall { _.max.isDefined })
            Some(Size(dimensions2D.map { _.max.get }))
        else
            None
    }
    
    /**
      * @return The minimum width of this stack size
      */
    def minWidth = width.min
    /**
      * @return The minimum height of this stack size
      */
    def minHeight = height.min
    
    /**
      * @return The optimal width of this stack size
      */
    def optimalWidth = width.optimal
    /**
      * @return The optimal height of this stack size
      */
    def optimalHeight = height.optimal
    
    /**
      * @return The maximum width of this size. None if not specified.
      */
    def maxWidth = width.max
    /**
      * @return The maximum heigth of this size. None if not specified.
      */
    def maxHeight = height.max
    
    /**
      * @return A copy of this size with no width limits
      */
    def withAnyWidth = mapWidth { _.noLimits }
    /**
      * @return A copy of this size with no height limits
      */
    def withAnyHeight = mapHeight { _.noLimits }
    
    /**
     * @return A copy of this size with no maximum width
     */
    def withNoMaxWidth = if (width.hasMax) mapWidth { _.noMax } else this
    /**
     * @return A copy of this size with no maximum height
     */
    def withNoMaxHeight = if (height.hasMax) mapHeight { _.noMax } else this
    /**
     * @return A copy of this size with no maximum width or height
     */
    def withNoMax = withNoMaxWidth.withNoMaxHeight
    
    /**
      * @return A copy of this size with low priority for both width and height
      */
    def withLowPriority = mapSides { _.withLowPriority }
    /**
      * @return A copy of this size that is more easily shrinked
      */
    def shrinking = mapSides { _.shrinking }
    /**
      * @return A copy of this size that is more easily expanded
      */
    def expanding = mapSides { _.expanding }
    
    /**
     * @return The components that form this stack size
     */
    @deprecated("Please use .dimensions instead", "v1.3")
    def components = Vector(width, height)
    
    
    // IMPLEMENTED    ----------------
    
    override def toInsets = StackInsets.symmetric(this)
    
    override def zeroDimension = StackLength.any
    
    override def toString = s"[$width, $height]"
    
    override def *(multi: Double) = StackSize(dimensions2D.map { _ * multi })
    
    override def +(other: StackSize) = mergeWith(other) { _ + _ }
    
    override def repr = this
    
    
    // OPERATORS    ------------------
    
    /**
      * @param size An increase in size
      * @return An increased version of this size
      */
    def +(size: Size) = mergeWith(size) { _ + _ }
    /**
      * @param insets A set of insets
      * @return This size extended with the specified insets
      */
    def +(insets: StackInsets) = StackSize(width + insets.horizontal, height + insets.vertical)
    /**
      * @param insets A set of insets
      * @return This size extended with the specified insets
      */
    def +(insets: Insets) = StackSize(width + insets.horizontal, height + insets.vertical)
    
    /**
      * @param size A decrease in size
      * @return A decreased version of this size
      */
    def -(size: Size) = this + (-size)
    
    
	// OTHER    ----------------------
    
    /**
      * @param axis Target axis
      * @return The length of this size along the specified axis
      */
    def along(axis: Axis2D) = axis match 
    {
        case X => width
        case Y => height
    }
    
    /**
      * @param axis Targeted axis
      * @return Length priority of this size for the specified axis
      */
    def priorityFor(axis: Axis2D) = along(axis).priority
    /**
      * @param axis Target axis
      * @param adjustment Proposed adjustment
      * @return Whether this size is considered of low priority for the specified axis and adjustment combination
      */
    def isLowPriorityFor(axis: Axis2D, adjustment: Double) = priorityFor(axis).isFirstAdjustedBy(adjustment)
    
    /**
      * @param w New width
      * @return A copy of this size with specified width
      */
    def withWidth(w: StackLength) = StackSize(w, height)
    /**
      * @param h New height
      * @return A copy of this size with specified height
      */
    def withHeight(h: StackLength) = StackSize(width, h)
    /**
      * @param side New length
      * @param axis Axis that specifies whether lenght is width or height
      * @return A copy of this size with new side
      */
    def withSide(side: StackLength, axis: Axis2D) = axis match 
    {
        case X => withWidth(side)
        case Y => withHeight(side)
    }
    
    /**
      * @param map A mapping function
      * @return A copy of this size with mapped width
      */
    def mapWidth(map: StackLength => StackLength) = withWidth(map(width))
    /**
      * @param map A mapping function
      * @return A copy of this size with mapped height
      */
    def mapHeight(map: StackLength => StackLength) = withHeight(map(height))
    /**
      * @param axis Target axis that determines mapped side
      * @param map A mapping function
      * @return A copy of this size with a mapped side
      */
    def mapSide(axis: Axis2D)(map: StackLength => StackLength) = axis match
    {
        case X => mapWidth(map)
        case Y => mapHeight(map)
    }
    /**
      * Maps both sides of this stack size
      * @param map A mapping function
      * @return A new size
      */
    def mapSides(map: StackLength => StackLength) = StackSize(dimensions2D.map(map))
    
    /**
      * Maps both sides of this stack size using specified function
      * @param f A function that takes both axis and length and returns a new length for that axis
      * @return A stack size with mapped sides
      */
    def map(f: (Axis2D, StackLength) => StackLength) = StackSize(f(X, width), f(Y, height))
    
    /**
      * @param priority Length priority
      * @param axis Targeted axis
      * @return A copy of this stack size with specified length priority on targeted axis
      */
    def withPriorityFor(priority: LengthPriority, axis: Axis2D) = mapSide(axis) { _.withPriority(priority) }
    /**
      * @param axis Target axis
      * @return A copy of this size with low priority for specified axis
      */
    def withLowPriorityFor(axis: Axis2D) = withPriorityFor(Low, axis)
    
    /**
      * @param length New fixed length
      * @param axis Target axis
      * @return A copy of this size with fixed length for specified side
      */
    def withFixedSide(length: Double, axis: Axis2D) = withSide(StackLength.fixed(length), axis)
    /**
      * @param w New fixed width
      * @return A copy of this size with fixed width
      */
    def withFixedWidth(w: Double) = withFixedSide(w, X)
    /**
      * @param h new fixed height
      * @return A copy of this size with fixed height
      */
    def withFixedHeight(h: Double) = withFixedSide(h, Y)
    
    /**
      * @param maxLength New maximum length
      * @param axis target axis
      * @return A copy of this size with new maximum length for specified side
      */
    def withMaxSide(maxLength: Double, axis: Axis2D) = mapSide(axis) { _.withMax(maxLength) }
    /**
      * @param maxW New maximum width
      * @return A copy of this size with specified maximum width
      */
    def withMaxWidth(maxW: Double) = withMaxSide(maxW, X)
    /**
      * @param maxH New maximum height
      * @return A copy of this size with specified maximum height
      */
    def withMaxHeight(maxH: Double) = withMaxSide(maxH, Y)
    /**
      * @param max New max size
      * @return A copy of this size with specified maximum
      */
    def withMax(max: Size) = withMaxWidth(max.width.toInt).withMaxHeight(max.height.toInt)
    
    /**
      * @param optimalLength New optimal length
      * @param axis Target axis
      * @return A copy of this size with specified optimal length for the specified axis
      */
    def withOptimalSide(optimalLength: Double, axis: Axis2D) = mapSide(axis) { _.withOptimal(optimalLength) }
    /**
      * @param optimalW New optimal width
      * @return A copy of this size with specified optimal width
      */
    def withOptimalWidth(optimalW: Double) = withOptimalSide(optimalW, X)
    /**
      * @param optimalH New optimal height
      * @return A copy of this size with specified optimal height
      */
    def withOptimalHeight(optimalH: Double) = withOptimalSide(optimalH, Y)
    /**
      * @param optimal New optimal size
      * @return A copy of this size with specified optimal size
      */
    def withOptimal(optimal: Size) = withOptimalWidth(optimal.width.toInt).withOptimalHeight(optimal.height.toInt)
    
    /**
      * @param minLength New minimum length
      * @param axis Target axis
      * @return A copy of this size with specified minimum length for the specified axis
      */
    def withMinSide(minLength: Double, axis: Axis2D) = mapSide(axis) { _.withMin(minLength) }
    /**
      * @param minW A new minimum width
      * @return A copy of this size with specified minimum width
      */
    def withMinWidth(minW: Double) = withMinSide(minW, X)
    /**
      * @param minH A new minimum height
      * @return A copy of this size with specified minimum height
      */
    def withMinHeight(minH: Double) = withMinSide(minH, Y)
    /**
      * @param min A new minimum size
      * @return A copy of this size with specified minimum size
      */
    def withMin(min: Size) = withMinWidth(min.width.toInt).withMinHeight(min.height.toInt)
    
    /**
      * @param size A limiting size
      * @return A copy of this size that always fits to the specified size
      */
    def limitedTo(size: Size) = map { (side, length) => length.within(size.along(side)) }
    
    /**
      * @param other Another size
      * @return A minimum between these two sizes
      */
    def min(other: StackSize) = mergeWith(other) { _ min _ }
    /**
      * @param other Another size
      * @return A maximum between these two sizes
      */
    def max(other: StackSize) = mergeWith(other) { _ max _ }
    /**
      * @param other Another size
      * @return A combination of these sizes that fulfills constraints of both sizes
      */
    def combine(other: StackSize) = mergeWith(other) { _ combineWith _ }
    
    /**
      * @param minimum Minimum allowed size
      * @param maximum Maximum allowed size
      * @return Whether this stack size contains a value that fits within those limits
      */
    def fitsWithin(minimum: Size, maximum: Size) = Axis2D.values.forall { axis =>
        along(axis).fitsWithin(minimum.along(axis), maximum.along(axis))
    }
    /**
      * @param maximum Maximum allowed size
      * @return Whether this stack size contains a value that fits under that maximum size
      */
    def fitsWithin(maximum: Size) = min.fitsWithin(maximum)
    /**
      * @param limits Another stack size
      * @return Whether there exists a value that fulfils both of these size requirements
      */
    def fitsWithin(limits: StackSize) = Axis2D.values
        .forall { axis => along(axis).fitsWithin(limits.along(axis)) }
    
    private def mergeWith[B](other: MultiDimensional[B])(f: (StackLength, B) => StackLength) =
        StackSize(dimensions2D.mergeWith(other.dimensions2D)(f))
}