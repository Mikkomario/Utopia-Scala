package utopia.paradigm.shape.shape2d.insets

import utopia.flow.collection.immutable.Pair
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Axis, Axis2D, Direction2D}
import utopia.paradigm.shape.template.{Dimensional, DimensionalBuilder, DimensionalFactory, Dimensions}

import scala.annotation.unchecked.uncheckedVariance
import scala.collection.mutable

/**
  * Common trait for factories which produce insets
  * @tparam L Type of inset lengths used
  * @tparam I Type of insets produced by this factory
  */
trait SidesFactory[-L, +I] extends DimensionalFactory[Pair[L], I]
{
    // ABSTRACT ---------------------------
    
    /**
      * Creates a item with the specified side lengths
      * @param sides Lengths for each side
      * @return An item with the specified lengths
      */
    def withSides(sides: Map[Direction2D, L]): I
    
    
    // IMPLEMENTED  -----------------------
    
    override def newBuilder = new SidedBuilder[L, I](this)
    
    override def empty = withSides(Map())
    
    override def apply(values: IndexedSeq[Pair[L]]): I = from(values)
    override def apply(values: Map[Axis, Pair[L]]): I = withSides(values.flatMap { case (axis, lengths) =>
        axis match {
            case axis: Axis2D => axis.directions.zip(lengths)
            case _ => None
        }
    })
    
    override def from(values: IterableOnce[Pair[L]]): I =
        withSides(Axis2D.values.zip(values).flatMap { case (axis, lengths) => axis.directions.zip(lengths)}.toMap)
    
    
    // OTHER    ---------------------------
    
    /**
      * Creates new insets
      * @param left Amount of insets on the left side
      * @param right Amount of insets on the right side
      * @param top Amount of insets on the top side
      * @param bottom Amount of insets on the bottom side
      * @return A new set of insets
      */
    def apply(left: L, right: L, top: L, bottom: L): I = withSides(
        Map(Direction2D.Left -> left, Direction2D.Right -> right, Up -> top, Down -> bottom))
    
    /**
      * Creates a set of insets where top = bottom and left = right
      * @param w The left & right inset
      * @param h The top & bottom inset
      * @return A new set of insets
      */
    def symmetric(w: L, h: L) = apply(w, w, h, h)
    /**
      * @param sideWidth The width of each side on these insets
      * @return Insets with all sides equal
      */
    def symmetric(sideWidth: L) = apply(sideWidth, sideWidth, sideWidth, sideWidth)
    /**
      * @param sideWidth The width of both of the targeted sides
      * @param axis Targeted axis (X|Y)
      * @return A set of either horizontal or vertical insets
      */
    def symmetric(sideWidth: L, axis: Axis2D) = withSides(axis.directions.map { d => d -> sideWidth }.toMap)
    
    /**
      * Creates a horizontal set of insets
      * @param left Left side
      * @param right Right side
      * @return New insets
      */
    def horizontal(left: L, right: L) = withSides(Map(Direction2D.Left -> left, Direction2D.Right -> right))
    /**
      * Creates a horizontal set of insets
      * @param w left / right side
      * @return New insets
      */
    def horizontal(w: L): I = horizontal(w, w)
    
    /**
      * Creates a vertical set of insets
      * @param top Top side
      * @param bottom Bottom side
      * @return New insets
      */
    def vertical(top: L, bottom: L) = withSides(Map(Up -> top, Down -> bottom))
    /**
      * Creates a vertical set of insets
      * @param h Top / bottom side
      * @return New insets
      */
    def vertical(h: L): I = vertical(h, h)
    
    /**
      * @param direction Target direction
      * @param amount length of inset
      * @return An inset with only one side
      */
    def towards(direction: Direction2D, amount: L) = withSides(Map(direction -> amount))
    
    /**
      * @param amount Length of inset
      * @return An inset with only left side
      */
    def left(amount: L) = towards(Direction2D.Left, amount)
    /**
      * @param amount Length of inset
      * @return An inset with only right side
      */
    def right(amount: L) = towards(Direction2D.Right, amount)
    /**
      * @param amount Length of inset
      * @return An inset with only top side
      */
    def top(amount: L) = towards(Up, amount)
    /**
      * @param amount Length of inset
      * @return An inset with only bottom side
      */
    def bottom(amount: L) = towards(Down, amount)
    
    /**
      * @param f A function that accepts a direction and generates the inset to apply towards that direction
      * @return A set of insets that consists of 4 'f' results
      */
    def fromFunction(f: Direction2D => L) = withSides(Direction2D.values.map { d => d -> f(d) }.toMap)
}

object SidedBuilder
{
    /**
      * A builder that yields an instance of Sides
      */
    type SidesBuilder[L] = SidedBuilder[L, Sides[L]]
}

/**
  * A builder used for building insets
  * @param factory Factory used for constructing the resulting insets
  * @tparam L Type of the inset lengths applied
  * @tparam I Type of the resulting insets
  */
class SidedBuilder[-L, +I](factory: SidesFactory[L, I]) extends DimensionalBuilder[Pair[L], I]
{
    // ATTRIBUTES   ---------------------
    
    // Unchecked variance because this value is protected and carefully handled
    private val buffer: mutable.Map[Direction2D, L @uncheckedVariance] = mutable.Map()
    
    
    // IMPLEMENTED  ---------------------
    
    override def update(axis: Axis, value: Pair[L]): Unit = axis match {
        case axis: Axis2D => update(axis, value)
        case _ => ()
    }
    
    override def clear() = buffer.clear()
    override def result() = factory.withSides(buffer.toMap)
    
    override def addOne(elem: Pair[L]) = {
        // If X-directions have not been specified yet, specifies those
        if (X.directions.exists { !buffer.contains(_) })
            buffer ++= X.directions.zip(elem)
        // Otherwise, if the Y-directions haven't been specified yet, specifies those
        else if (Y.directions.exists { !buffer.contains(_) })
            buffer ++= Y.directions.zip(elem)
        // Won't accept elements after both axes have been specified
        this
    }
    
    
    // OTHER    ------------------------
    
    /**
      * Updates a single direction's side
      * @param direction Targeted direction / side
      * @param length Assigned inset length
      * @return This builder
      */
    def update(direction: Direction2D, length: L) = {
        buffer += (direction -> length)
        this
    }
    /**
      * Updates the lengths along a single axis
      * @param axis Targeted axis
      * @param lengths The inset lengths to assign
      *                (first towards the negative direction, then towards the positive direction)
      * @return This builder
      */
    def update(axis: Axis2D, lengths: Pair[L]) = {
        buffer ++= axis.directions.zip(lengths)
        this
    }
    
    /**
      * @param top Top inset to assign
      * @return This builder
      */
    def setTop(top: L) = update(Up, top)
    /**
      * @param bottom Bottom inset to assign
      * @return This builder
      */
    def setBottom(bottom: L) = update(Down, bottom)
    /**
      * @param left Left inset to assign
      * @return This builder
      */
    def setLeft(left: L) = update(Direction2D.Left, left)
    /**
      * @param right Right inset to assign
      * @return This builder
      */
    def setRight(right: L) = update(Direction2D.Right, right)
    
    /**
      * Assigns the same length to both directions along a specific axis
      * @param axis Targeted axis
      * @param length Length to assign to insets on the both sides along that axis
      * @return This builder
      */
    def setSymmetric(axis: Axis2D, length: L) = update(axis, Pair.twice(length))
    /**
      * Assigns symmetric left and right inset
      * @param length Inset to assign to both sides
      * @return This builder
      */
    def setHorizontallySymmetric(length: L) = setSymmetric(X, length)
    /**
      * Assigns symmetric top and bottom inset
      * @param length Inset to assign to both sides
      * @return This builder
      */
    def setVerticallySymmetric(length: L) = setSymmetric(Y, length)
    
    /**
      * Removes an assigned direction
      * @param direction Direction to remove
      * @return This builder
      */
    def -=(direction: Direction2D) = {
        buffer -= direction
        this
    }
    /**
      * Removes all assignments along an axis
      * @param axis Axis to remove / set to zero
      * @return This builder
      */
    def -=(axis: Axis2D): SidedBuilder[L, I] = {
        axis.directions.foreach { this -= _ }
        this
    }
}

/**
* Represents an item which specifies a (length) value for 0-4 2-dimensional sides (top, bottom, left and/or right)
* @author Mikko Hilpinen
* @since Previously InsetsLike from Genesis, 25.3.2019. Current version added 16.1.2024 for v1.5.
  * @tparam L Type of lengths used
  * @tparam Repr Type of the concrete implementation of this trait
**/
trait SidesLike[L, +Repr] extends HasSides[L] with Dimensional[Pair[L], Repr]
{
    // ABSTRACT ------------------
    
    /**
      * @param sides New lengths for sides
      * @return A new copy of this item with the specified lengths
      */
    protected def withSides(sides: Map[Direction2D, L]): Repr
    
    
	// COMPUTED    ---------------
    
    /**
      * @return A copy of these insets where up is down and left is right
      */
    def opposite = withSides(sides.map { case (k, v) => k.opposite -> v })
    
    /**
      * @return A copy of these insets where left and right have been swapped
      */
    def hMirrored = mirroredAlong(X)
    /**
      * @return A copy of these insets where top and bottom have been swapped
      */
    def vMirrored = mirroredAlong(Y)
    
    /**
      * @return A copy of these insets with only horizontal components (left & right)
      */
    def onlyHorizontal = onlyAxis(X)
    /**
      * @return A copy of these insets with only vertical components (top & bottom)
      */
    def onlyVertical = onlyAxis(Y)
    
    /**
      * @return A copy of these insets without any horizontal components (left or right)
      */
    def withoutHorizontal = withoutAxis(X)
    /**
      * @return A copy of these insets without any vertical components (top or bottom)
      */
    def withoutVertical = withoutAxis(Y)
    
    
    // IMPLEMENTED  --------------
    
    override def withDimensions(newDimensions: Dimensions[Pair[L]]) = {
        val horizontal = newDimensions.x
        val vertical = newDimensions.y
        withSides(Map(Direction2D.Left -> horizontal.first, Direction2D.Right -> horizontal.second,
            Up -> vertical.first, Down -> vertical.second))
    }
    
    
    // OPERATORS    --------------
    
    /**
      * Adds a number of sides to this set, **overwriting** the existing definitions.
      * @param sides Sides to place
      * @return Copy of these sides with some overwritten with the specified set
      */
    def ++(sides: HasSides[L]) = withSides(this.sides ++ sides.sides)
    
    /**
      * @param direction Direction to drop from these insets
      * @return A copy of these insets without an inset for the specified direction
      */
    def -(direction: Direction2D) = withSides(sides - direction)
    /**
      * @param axis Axis to omit from these insets
      * @return Copy of these insets without values along the specified axis
      */
    def -(axis: Axis2D) = withSides(sides.filterNot { _._1.axis == axis })
    /**
      * @param directions Directions to exclude from these insets
      * @return Copy of these insets without the specified directions included
      */
    def --(directions: IterableOnce[Direction2D]) = withSides(sides -- directions)
    
    
    // OTHER    ------------------
    
    /**
     * @param axis Target axis
     * @return A copy of these insets where values on targeted axis are swapped
     */
    def mirroredAlong(axis: Axis2D) =
        withSides(sides.map { case (k, v) => (if (k.axis == axis) k.opposite else k) -> v })
    
    /**
     * @param direction The direction taken away from these insets
     * @return A copy of these insets without specified direction
     */
    def withoutSide(direction: Direction2D) = withSides(sides - direction)
    /**
      * @param directions Directions to exclude from these insets
      * @return A copy of these insets without the specified directions included
      */
    def withoutSides(directions: IterableOnce[Direction2D]) = withSides(sides -- directions)
    
    /**
      * @param axis Targeted axis
      * @return A copy of these insets with values only on the specified axis (Eg. for X-axis would only contain left and right)
      */
    def onlyAxis(axis: Axis) = withSides(sides.view.filterKeys { _.axis == axis }.toMap)
    /**
      * @param axis Targeted axis
      * @return A copy of these insets without any values for the specified axis
      */
    def withoutAxis(axis: Axis) = withSides(sides.view.filterKeys { _.axis != axis }.toMap)
    
    /**
      * Replaces one side of these insets
      * @param side Targeted side
      * @param amount New length for that side insets
      * @return A copy of these insets with replaced side
      */
    def withSide(side: Direction2D, amount: L) = withSides(sides + (side -> amount))
    
    /**
      * @param amount New top length
      * @return A copy of these insets with new top length
      */
    def withTop(amount: L) = withSide(Up, amount)
    /**
      * @param amount New bottom length
      * @return A copy of these insets with new bottom length
      */
    def withBottom(amount: L) = withSide(Down, amount)
    /**
      * @param amount New left length
      * @return A copy of these insets with new left length
      */
    def withLeft(amount: L) = withSide(Direction2D.Left, amount)
    /**
      * @param amount New right length
      * @return A copy of these insets with new right length
      */
    def withRight(amount: L) = withSide(Direction2D.Right, amount)
    
    /**
      * @param axis Targeted axis
      * @param negative The length to place towards the negative direction (top or left)
      * @param positive The length to place towards the positive direction (bottom ro right)
      * @return Copy of these sides with the specified axis updated
      */
    def withAxis(axis: Axis2D, negative: L, positive: L) =
        withSides(sides ++ axis.directions.zip(Pair(negative, positive)))
    /**
      * @param axis Targeted axis
      * @param length The side-length to place to **both ends** along that axis
      * @return Copy of these sides with the specified axis updated
      */
    def withAxis(axis: Axis2D, length: L) = withSides(sides ++ axis.directions.map { _ -> length })
    
    /**
      * @param left Length to assign to the left side
      * @param right Length to assign to the right side
      * @return Copy of this item with the specified lengths
      */
    def withHorizontal(left: L, right: L) = withAxis(X, left, right)
    /**
      * @param width Length to assign to both the left and the right side
      * @return Copy of this item with the specified lengths
      */
    def withHorizontal(width: L) = withAxis(X, width)
    /**
      * @param top Length to assign to the left side
      * @param bottom Length to assign to the right side
      * @return Copy of this item with the specified lengths
      */
    def withVertical(top: L, bottom: L) = withAxis(Y, top, bottom)
    /**
      * @param height Length to assign to both the top and the bottom side
      * @return Copy of this item with the specified lengths
      */
    def withVertical(height: L) = withAxis(Y, height)
    
    /**
      * Performs a mapping function over a single side in these insets
      * @param side Targeted side
      * @param f A mapping function
      * @return Copy of these insets with mapped side
      */
    def mapSide(side: Direction2D)(f: L => L) = withSide(side, f(apply(side)))
    
    /**
      * Modifies the top of these insets
      * @param f Mapping function
      * @return A copy of these insets with mapped top
      */
    def mapTop(f: L => L) = mapSide(Up)(f)
    /**
      * Modifies the bottom of these insets
      * @param f Mapping function
      * @return A copy of these insets with mapped bottom
      */
    def mapBottom(f: L => L) = mapSide(Down)(f)
    /**
      * Modifies the left of these insets
      * @param f Mapping function
      * @return A copy of these insets with mapped left
      */
    def mapLeft(f: L => L) = mapSide(Direction2D.Left)(f)
    /**
      * Modifies the right of these insets
      * @param f Mapping function
      * @return A copy of these insets with mapped right
      */
    def mapRight(f: L => L) = mapSide(Direction2D.Right)(f)
    
    /**
      * Maps multiple sides of these insets
      * @param sides Targeted sides
      * @param f Mapping function
      * @return A copy of these insets with mapped sides
      */
    def mapSides(sides: IterableOnce[Direction2D])(f: L => L) =
        withSides(this.sides ++ sides.iterator.map { d => d -> f(apply(d)) })
    /**
      * Maps sides along the specified axis
      * @param axis Targeted axis
      * @param f Mapping function
      * @return A copy of these insets with two sides mapped
      */
    def mapAxis(axis: Axis2D)(f: L => L) = mapSides(axis.directions)(f)
    /**
      * Maps left & right
      * @param f Mapping function
      * @return A mapped copy of these insets
      */
    def mapHorizontal(f: L => L) = mapAxis(X)(f)
    /**
      * Maps top & bottom
      * @param f Mapping function
      * @return A mapped copy of these insets
      */
    def mapVertical(f: L => L) = mapAxis(Y)(f)
    
    /**
      * Maps the all 4 sides of this item
      * @param f A mapping function
      * @return copy of this item with each side mapped
      */
    def map(f: L => L) = withSides(Direction2D.values.map { d => d -> f(apply(d)) }.toMap)
    /**
      * Maps the sides which have a defined value
      * @param f A mapping function
      * @return Copy of this item with mapped sides
      */
    def mapDefined(f: L => L) = withSides(sides.view.mapValues(f).toMap)
    /**
      * @param f A mapping function which may yield None
      * @return Copy of these sides where each **defined** side is mapped.
      *         Sides mapping to None will be removed (i.e. will appear as zero values)
      */
    def flatMapDefined(f: L => Option[L]) = withSides(sides.flatMap { case (d, l) => f(l).map { d -> _ } })
    /**
      * Maps all sides of these insets
      * @param f A mapping function which accepts the direction of a side and the length of that side
      * @return A mapped copy of these insets
      */
    def mapWithDirection(f: (Direction2D, L) => L) = withSides(sides.map { case (dir, len) => dir -> f(dir, len) })
    
    /**
      * Merges these sides with another set of sides
      * @param other Another set of sides
      * @param f A function that merges the values of these sides
      * @tparam A Type of the values in the other set
      * @return Merge results
      */
    def mergeWith[A](other: HasSides[A])(f: (L, A) => L) =
        withSides((sides.keySet ++ other.sides.keySet).map { d => d -> f(apply(d), other(d)) }.toMap)
    
    /**
      * Maps these sides to another data type
      * @param f Mapping function to apply
      * @tparam B Type of mapping results
      * @return Mapped copy of these sides
      */
    def mapTo[B](f: L => B) = Sides(f(zeroLength))(sides.view.mapValues(f).toMap)
}