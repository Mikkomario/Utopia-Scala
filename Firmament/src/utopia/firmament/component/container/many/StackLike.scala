package utopia.firmament.component.container.many

import utopia.firmament.component.{AreaOfItems, HasMutableBounds}
import utopia.firmament.component.stack.{StackSizeCalculating, Stackable, StackableWrapper}
import utopia.firmament.controller.Stacker
import utopia.firmament.model.enumeration.StackLayout
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.{Bounds, Point, Size}
import utopia.firmament.model.stack.StackLength

/**
* A stack holds multiple stackable components in a stack-like manner either horizontally or vertically
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait StackLike[C <: Stackable] extends MultiContainer[C] with StackSizeCalculating with AreaOfItems[C] with Stackable
{
    // ABSTRACT -------------------------
    
    /**
      * @return The direction of this stack (components will be placed along this direction)
      */
    def direction: Axis2D
    /**
      * @return The layout this stack uses
      */
    def layout: StackLayout
    /**
      * @return The margin between components
      */
    def margin: StackLength
    /**
      * @return The cap at each end of this stack
      */
    def cap: StackLength
    
    
    // COMPUTED    ----------------------
    
    /**
      * @return The current length of this stack
      */
    def length = size(direction)
    /**
      * @return The current breadth (perpendicular length) of this stack
      */
    def breadth = size(direction.perpendicular)
    
    /**
     * The length (min, optimal, max) of this stack
     */
    def stackLength = stackSize(direction)
    /**
     * The breadth (min, optimal, max) of this stack
     */
    def stackBreadth = stackSize(direction.perpendicular)
    
    
    // IMPLEMENTED    -------------------
    
    override def toString = direction match
    {
        case X => s"Row[$count]"
        case Y => s"Column[$count]"
    }
    
    override def calculatedStackSize = Stacker.calculateStackSize(components.map { _.stackSize }, direction,
        margin, cap, layout)
    
    override def updateLayout() =
    {
        // Wraps the components in order to delay changes
        val wrappedComponents = components.map { new DelayedBoundsUpdate(_) }
        
        // Positions the components using a stacker
        Stacker(wrappedComponents, Bounds(Point.origin, size), stackLength.optimal, direction, margin, cap, layout)
            
        // Finally applies the changes
        wrappedComponents.foreach { _.updateBounds() }
    }
    
    /**
      * Finds the area of a single element in this stack, including the area around the object
      * @param item An item in this stack
      * @return The bounds around the item. None if the item isn't in this stack
      */
    override def areaOf(item: C) =
    {
        // Caches components so that indexes won't change in between
        val c = components
        c.optionIndexOf(item).map { i =>
            if (c.size == 1)
                Bounds(Point.origin, size)
            else {
                // Includes half of the area between items (if there is no item, uses cap)
                val top = if (i > 0) (item.position(direction) - c(i - 1).maxAlong(direction)) / 2 else
                    item.position(direction)
                val bottom = if (i < c.size - 1) (c(i + 1).position(direction) - item.maxAlong(direction)) / 2 else
                    length - item.maxAlong(direction)
                
                // Also includes the whole stack breadth
                Bounds(item.position - direction(top), item.size.withDimension(direction.perpendicular(breadth)) +
                    direction(top + bottom))
            }
        }
    }
    
    /**
      * Finds the item that's neares to a <b>relative</b> point
      * @param relativePoint A point relative to this Stack's position ((0, 0) = stack origin)
      * @return The component that's nearest to the provided point. None if this stack is empty
      */
    override def itemNearestTo(relativePoint: Point) =
    {
        val p = relativePoint(direction)
        val c = components
        // Finds the first item past the relative point
        c.indexWhereOption { _.position(direction) > p }.map { nextIndex =>
            // Selects the next item if a) it's the first item or b) it's closer to point than the previous item
            if (nextIndex == 0 || c(nextIndex).position(direction) - p < p - c(nextIndex - 1).maxAlong(direction))
                c(nextIndex)
            else
                c(nextIndex - 1)
            
        }.orElse(c.lastOption)
    }
    
    
    // OTHER    -----------------------
    
    /**
      * @param component A component within this stack
      * @return Current index of the component. None if there's no such component in this stack
      */
    def indexOf(component: Any) = components.indexOf(component)
}

private class DelayedBoundsUpdate[C <: Stackable](val source: C) extends HasMutableBounds with StackableWrapper
{
    // ATTRIBUTES    -------------------
    
    private var nextPosition: Option[Point] = None
    private var nextSize: Option[Size] = None
    
    
    // IMPLEMENTED    -----------------
    
    override def bounds = Bounds(position, size)
    override def bounds_=(b: Bounds) =
    {
        position = b.position
        size = b.size
    }
    
    override def position = nextPosition getOrElse source.position
    override def position_=(p: Point) = nextPosition = Some(p)
    
    override def size = nextSize getOrElse source.size
    override def size_=(s: Size) = nextSize = Some(s)
    
    override protected def wrapped = source
    
    
    // OTHER    -----------------------
    
    def updateBounds() =
    {
        nextPosition filterNot { _ ~== source.position } foreach { source.position = _ }
        nextPosition = None
        
        nextSize filterNot { _ ~== source.size } foreach { source.size = _ }
        nextSize = None
    }
}

