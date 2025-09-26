package utopia.paradigm.shape.shape2d.insets

import utopia.flow.collection.immutable.Pair
import utopia.flow.util.Mutate
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
	 * @param sideWidth The width applied to all sides, symmetrically
	 * @return A symmetric set of sides where each has the specified length
	 */
	def apply(sideWidth: L): I = symmetric(sideWidth)
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
    def symmetric(sideWidth: L): I = apply(sideWidth, sideWidth, sideWidth, sideWidth)
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
    def apply(direction: Direction2D, amount: L) = withSides(Map(direction -> amount))
	@deprecated("Renamed to .apply(Direction2D, L)", "v1.7.3")
	def towards(direction: Direction2D, amount: L) = apply(direction, amount)
    
    /**
      * @param amount Length of inset
      * @return An inset with only left side
      */
    def left(amount: L) = apply(Direction2D.Left, amount)
    /**
      * @param amount Length of inset
      * @return An inset with only right side
      */
    def right(amount: L) = apply(Direction2D.Right, amount)
    /**
      * @param amount Length of inset
      * @return An inset with only top side
      */
    def top(amount: L) = apply(Up, amount)
    /**
      * @param amount Length of inset
      * @return An inset with only bottom side
      */
    def bottom(amount: L) = apply(Down, amount)
    
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
    def onlyHorizontal = only(X)
    /**
      * @return A copy of these insets with only vertical components (top & bottom)
      */
    def onlyVertical = only(Y)
    
    /**
      * @return A copy of these insets without any horizontal components (left or right)
      */
    def withoutHorizontal = without(X)
    /**
      * @return A copy of these insets without any vertical components (top or bottom)
      */
    def withoutVertical = without(Y)
	
	/**
	 * @return A copy of this item with only the top remaining
	 */
	def onlyTop = only(Up)
	/**
	 * @return A copy of this item with only the bottom remaining
	 */
	def onlyBottom = only(Down)
	/**
	 * @return A copy of this item with only the left side remaining
	 */
	def onlyLeft = only(Direction2D.Left)
	/**
	 * @return A copy of this item with only the right side remaining
	 */
	def onlyRight = only(Direction2D.Right)
    
    
    // IMPLEMENTED  --------------
    
    override def withDimensions(newDimensions: Dimensions[Pair[L]]) = {
        val horizontal = newDimensions.x
        val vertical = newDimensions.y
        withSides(Map(Direction2D.Left -> horizontal.first, Direction2D.Right -> horizontal.second,
            Up -> vertical.first, Down -> vertical.second))
    }
    
    
    // OTHER    ------------------
    
    /**
     * @param axis Target axis
     * @return A copy of these insets where values on targeted axis are swapped
     */
    def mirroredAlong(axis: Axis2D) =
        withSides(sides.map { case (k, v) => (if (k.axis == axis) k.opposite else k) -> v })
    
    /**
     * @param side The direction taken away from these insets
     * @return A copy of these insets without specified direction
     */
    def without(side: Direction2D) = filterNot { (dir, _) => dir == side }
	@deprecated("Renamed to .without(Direction2D)", "v1.7.3")
	def withoutSide(direction: Direction2D) = without(direction)
    /**
      * @param directions Directions to exclude from these insets
      * @return A copy of these insets without the specified directions included
      */
    def withoutSides(directions: IterableOnce[Direction2D]) = {
	    val dirsToRemove = Set.from(directions)
	    if (dirsToRemove.isEmpty)
		    self
	    else
	        filterNot { (dir, _) => dirsToRemove.contains(dir) }
    }
	/**
	 * @param axis Targeted axis
	 * @return A copy of these insets without any values for the specified axis
	 */
	def without(axis: Axis) = filterNot { (dir, _) => dir.axis == axis }
	@deprecated("Renamed to .without(Axis)")
	def withoutAxis(axis: Axis) = without(axis)
	
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
      * Replaces one side of these insets
      * @param side Targeted side
      * @param amount New length for that side insets
      * @return A copy of these insets with replaced side
      */
    def withSide(side: Direction2D, amount: L) = withSides(sides + (side -> amount))
	/**
	 * @param side Side to specify
	 * @param amount Length of the specified side
	 * @param exclusive Whether this should be the only applied side (default = false)
	 * @return A copy of this item with the specified side set to the specified length
	 */
	def withSide(side: Direction2D, amount: L, exclusive: Boolean): Repr = {
		if (exclusive)
			withSides(Map(side -> amount))
		else
			withSide(side, amount)
	}
    
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
	 * @param side The only side to preserve
	 * @return A copy of this item with only the specified side preserved (and others at zero)
	 */
	def only(side: Direction2D) = filter { (dir, _) => dir == side }
	/**
	 * @param axis Targeted axis
	 * @return A copy of these insets with values only on the specified axis (Eg. for X-axis would only contain left and right)
	 */
	def only(axis: Axis) = filter { (dir, _) => dir.axis == axis }
	@deprecated("Renamed to .only(Axis)", "v1.7.3")
	def onlyAxis(axis: Axis) = only(axis)
	
	/**
	 * Adds a number of sides to this set, **overwriting** the existing definitions.
	 * @param sides Sides to place
	 * @return Copy of these sides with some overwritten with the specified set
	 */
	def ++(sides: HasSides[L]) = withSides(this.sides ++ sides.sides)
	
	/**
	 * @param side Side to drop from this item
	 * @return A copy of this item without the specified side
	 */
	def -(side: Direction2D) = without(side)
	/**
	 * @param axis Axis to omit from this item
	 * @return Copy of this item without values along the specified axis
	 */
	def -(axis: Axis) = without(axis)
	/**
	 * @param sides Directions to exclude from these insets
	 * @return Copy of these insets without the specified directions included
	 */
	def --(sides: IterableOnce[Direction2D]) = withoutSides(sides)
	
	/**
	 * Maps the all 4 sides of this item
	 * @param f A mapping function
	 * @return copy of this item with each side mapped
	 */
	def map(f: Mutate[L]): Repr = mapWithSide { case (_, len) => f(len) }
	/**
	 * Maps the all 4 sides of this item
	 * @param f A mapping function. Accepts the side to modify, as well as the existing length of that side.
	 *          Yields the new length for that side.
	 * @return copy of this item with each side mapped
	 */
	def mapWithSide(f: (Direction2D, L) => L): Repr =
		withSides(Direction2D.values.iterator.map { side => side -> f(side, apply(side)) }.toMap)
	
	/**
      * Performs a mapping function over a single side in these insets
      * @param side Targeted side
      * @param f A mapping function
      * @return Copy of these insets with mapped side
      */
    def mapSide(side: Direction2D)(f: L => L) =
	    mapWithSide { (target, length) => if (target == side) f(length) else length }
    
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
    def mapSides(sides: IterableOnce[Direction2D])(f: L => L) = {
        val targetedSides = Set.from(sides)
	    if (targetedSides.isEmpty)
		    self
	    else
		    mapWithSide { (target, len) => if (targetedSides.contains(target)) f(len) else len }
    }
	
	/**
      * Maps sides along the specified axis
      * @param axis Targeted axis
      * @param f Mapping function
      * @return A copy of these insets with two sides mapped
      */
    def mapAxis(axis: Axis2D)(f: L => L) = mapWithSide { (side, len) => if (side.axis == axis) f(len) else len }
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
    @deprecated("Renamed to .mapWithSide(...)", "v1.7.3")
    def mapWithDirection(f: (Direction2D, L) => L) = mapWithSide(f)
	
	/**
	 * Maps these sides to another data type
	 * @param f Mapping function to apply
	 * @tparam B Type of mapping results
	 * @return Mapped copy of these sides
	 */
	def mapTo[B](f: L => B) = Sides(f(zeroLength))(sides.view.mapValues(f).toMap)
	
	/**
	 * @param f A filtering function applied to each side.
	 *          Accepts the applicable direction & side length. Yields whether the specified side should be preserved.
	 * @return A copy of this item only containing the sides accepted by the specified filter.
	 */
	def filter(f: (Direction2D, L) => Boolean) = withSides(sides.filter { case (dir, len) => f(dir, len) })
	/**
	 * @param f A filtering function applied to each side.
	 *          Accepts the applicable direction & side length. Yields whether the specified side should be removed.
	 * @return A copy of this item only containing the sides not fulfilling the specified filter.
	 */
	def filterNot(f: (Direction2D, L) => Boolean) = filter { !f(_, _) }
	
    /**
      * Merges these sides with another set of sides
      * @param other Another set of sides
      * @param f A function that merges the values of these sides
      * @tparam A Type of the values in the other set
      * @return Merge results
      */
    def mergeWith[A](other: HasSides[A])(f: (L, A) => L) =
        withSides((sides.keySet ++ other.sides.keySet).map { d => d -> f(apply(d), other(d)) }.toMap)
}