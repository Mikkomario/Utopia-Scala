package utopia.paradigm.shape.shape2d

import utopia.flow.operator.LinearScalable
import utopia.flow.util.CollectionExtensions._
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis2D, Direction2D}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}

import scala.collection.immutable.HashMap

trait InsetsFactory[L, S, +Repr, +I <: InsetsLike[L, S, Repr]]
{
    // ABSTRACT ---------------------------
    
    /**
      * Creates a new set of insets
      * @param amounts Lengths of each side in these insets
      * @return A set of insets with specified lengths
      */
    def apply(amounts: Map[Direction2D, L]): I
    
    
    // OTHER    ---------------------------
    
    /**
      * Creates new insets
      * @param left Amount of insets on the left side
      * @param right Amount of insets on the right side
      * @param top Amount of insets on the top side
      * @param bottom Amount of insets on the bottom side
      * @return A new set of insets
      */
    def apply(left: L, right: L, top: L, bottom: L): I = apply(
        HashMap(Direction2D.Left -> left, Direction2D.Right -> right, Up -> top, Down -> bottom))
    
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
    def symmetric(sideWidth: L, axis: Axis2D) = apply(axis.directions.map { d => d -> sideWidth }.toMap)
    
    /**
      * Creates a horizontal set of insets
      * @param left Left side
      * @param right Right side
      * @return New insets
      */
    def horizontal(left: L, right: L) = apply(HashMap(Direction2D.Left -> left, Direction2D.Right -> right))
    
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
    def vertical(top: L, bottom: L) = apply(HashMap(Up -> top, Down -> bottom))
    
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
    def towards(direction: Direction2D, amount: L) = apply(HashMap(direction -> amount))
    
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
}

/**
* Insets can be used for describing an area around a component (top, bottom, left and right)
* @author Mikko Hilpinen
* @since Genesis 25.3.2019
**/
trait InsetsLike[L, +S, +Repr] extends LinearScalable[Repr]
{
    // ABSTRACT ------------------
    
    /**
      * @return Lengths of each side in these insets
      */
    def amounts: Map[Direction2D, L]
    
    /**
      * @param newAmounts New lengths
      * @return A new copy of these insets with specified lengths
      */
    protected def makeCopy(newAmounts: Map[Direction2D, L]): Repr
    
    /**
      * @return A zero length item
      */
    protected def makeZero: L
    
    /**
      * @param first The first item
      * @param second The second item
      * @return Combination (+) of these two items
      */
    protected def combine(first: L, second: L): L
    
    /**
      * @param a The item to multiply
      * @param multiplier Multiplier
      * @return A multiplied copy of the item
      */
    protected def multiply(a: L, multiplier: Double): L
    
    /**
      * Combines two lengths into a size
      * @param horizontal Horizontal length
      * @param vertical Vertical length
      * @return A size
      */
    protected def make2D(horizontal: L, vertical: L): S
    
    
	// COMPUTED    ---------------
    
    /**
      * @return Lengths of all the sides in these insets
      */
    def sides = amounts.values.toVector
    
    /**
     * @return Insets for the left side
     */
    def left = apply(Direction2D.Left)
    
    /**
     * @return Insets for the right side
     */
    def right = apply(Direction2D.Right)
    
    /**
     * @return Insets for the top side
     */
    def top = apply(Up)
    
    /**
     * @return Insets for the bottom side
     */
    def bottom = apply(Down)
    
    /**
     * @return Total length of this inset's horizontal components
     */
    def horizontal = along(X)
    
    /**
     * @return Total length of this inset's vertical components
     */
    def vertical = along(Y)
    
    /**
     * The total size of these insets
     */
    def total = make2D(horizontal, vertical)
    
    /**
      * @return A copy of these insets where up is down and left is right
      */
    def opposite = makeCopy(amounts.map { case (k, v) => k.opposite -> v })
    
    /**
      * @return A copy of these insets where left and right have been swapped
      */
    def hMirrored = mirroredAlong(X)
    
    /**
      * @return A copy of these insets where top and bottom have been swapped
      */
    def vMirrored = mirroredAlong(Y)
    
    /**
      * @return The average width of these insets
      */
    def average = amounts.values.reduceOption(combine).map { multiply(_, 0.25) }.getOrElse(makeZero)
    
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
    
    override def toString = s"[${amounts.map { case (d, l) => s"$d:$l" }.mkString(", ")}]"
    
    /**
      * Multiplies each side of these insets
      * @param multi A multiplier
      * @return Multiplied insets
      */
    override def *(multi: Double) = makeCopy(amounts.map { case (side, length) => side -> multiply(length, multi) })
    
    
    // OPERATORS    --------------
    
    /**
     * @param multi A multiplier (may be different for different axes)
     * @return Multiplied copy of these insets
     */
    def *(multi: Vector2D) = makeCopy(amounts.map { case (side, length) =>
        side -> multiply(length, multi.along(side.axis)) })
    
    /**
      * Adds two insets together
      * @param other Another insets
      * @return A combination of these two insets
      */
    def +(other: InsetsLike[L, _, _]) = makeCopy(amounts.mergeWith[L](other.amounts)(combine))
    
    /**
      * Subtracts insets from each other
      * @param other Another insets
      * @return A subtraction of these two insets
      */
    def -(other: InsetsLike[L, _, _]) =
        makeCopy(amounts.mergeWith[L](other.amounts) { (a, b) => combine(a, multiply(b, -1)) })
    
    /**
      * @param direction Direction to drop from these insets
      * @return A copy of these insets without an inset for the specified direction
      */
    def -(direction: Direction2D) = makeCopy(amounts - direction)
    
    
    // OTHER    ------------------
    
    /**
      * @param direction Target direction
      * @return The length of these insets towards that direction
      */
    def apply(direction: Direction2D) = amounts.getOrElse(direction, makeZero)
    
    /**
     * @param axis Target axis
     * @return Total length of these insets along specified axis
     */
    def along(axis: Axis2D) = amounts.view.filterKeys { _.axis == axis }.values.reduceOption(combine).getOrElse(makeZero)
    
    /**
      * @param axis Target axis
      * @return The two sides of insets along that axis (FFor x-axis, returns left -> right and for y-axis top -> bottom)
      */
    def sidesAlong(axis: Axis2D) = axis match
    {
        case X => left -> right
        case Y => top -> bottom
    }
    
    /**
     * @param axis Target axis
     * @return A copy of these insets where values on targeted axis are swapped
     */
    def mirroredAlong(axis: Axis2D) = makeCopy(amounts.map { case (k, v) => (if (k.axis == axis) k.opposite else k) -> v })
    
    /**
     * @param direction The direction taken away from these insets
     * @return A copy of these insets without specified direction
     */
    def withoutSide(direction: Direction2D) = makeCopy(amounts - direction)
    
    /**
      * @param directions Directions to exclude from these insets
      * @return A copy of these insets without the specified directions included
      */
    def withoutSides(directions: IterableOnce[Direction2D]) = makeCopy(amounts -- directions)
    
    /**
      * @param axis Targeted axis
      * @return A copy of these insets with values only on the specified axis (Eg. for X-axis would only contain left and right)
      */
    def onlyAxis(axis: Axis2D) = makeCopy(amounts.view.filterKeys { _.axis == axis }.toMap)
    
    /**
      * @param axis Targeted axis
      * @return A copy of these insets without any values for the specified axis
      */
    def withoutAxis(axis: Axis2D) = onlyAxis(axis.perpendicular)
    
    /**
      * Replaces one side of these insets
      * @param side Targeted side
      * @param amount New length for that side insets
      * @return A copy of these insets with replaced side
      */
    def withSide(side: Direction2D, amount: L) = makeCopy(amounts + (side -> amount))
    
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
    def mapSides(sides: IterableOnce[Direction2D])(f: L => L) = makeCopy(amounts ++
        sides.iterator.map { d => d -> f(apply(d)) })
    
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
      * Maps all sides in these insets
      * @param f A mapping function
      * @return copy of these insets with each side mapped
      */
    def map(f: L => L) = mapSides(Direction2D.values)(f)
}