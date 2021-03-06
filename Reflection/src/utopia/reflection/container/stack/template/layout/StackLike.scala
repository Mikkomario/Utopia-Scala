package utopia.reflection.container.stack.template.layout

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.template.layout.stack.{StackSizeCalculating, Stackable, StackableWrapper}
import utopia.reflection.component.template.layout.{Area, AreaOfItems}
import utopia.reflection.container.stack.template.MultiStackContainer
import utopia.reflection.container.stack.{StackLayout, Stacker}
import utopia.reflection.shape.stack.StackLength

/**
* A stack holds multiple stackable components in a stack-like manner either horizontally or vertically
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait StackLike[C <: Stackable] extends MultiStackContainer[C] with StackSizeCalculating with AreaOfItems[C]
{
	// ATTRIBUTES    --------------------
    
    private var _components = Vector[StackItem[C]]()
    
    
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
    def length = size.along(direction)
    /**
      * @return The current breadth (perpendicular length) of this stack
      */
    def breadth = size.perpendicularTo(direction)
    
    /**
     * The length (min, optimal, max) of this stack
     */
    def stackLength = stackSize.along(direction)
    /**
     * The breadth (min, optimal, max) of this stack
     */
    def stackBreadth = stackSize.perpendicularTo(direction)
    
    
    // IMPLEMENTED    -------------------
    
    override def components = _components map { _.source }
    
    override def insert(component: C, index: Int) =
    {
        _components = _components.inserted(new StackItem[C](component), index)
        super.insert(component, index)
    }
    
    override def -=(component: C) =
    {
        _components = _components.filterNot { _.source == component }
        super.-=(component)
    }
    
    def calculatedStackSize = Stacker.calculateStackSize(
        _components.filter { _.visible }.map { _.stackSize }, direction, margin, cap, layout)
    
    def updateLayout() =
    {
        // Positions the components using a stacker
        Stacker(_components, Bounds(Point.origin, size), stackLength.optimal, direction, margin, cap, layout)
            
        // Finally applies the changes
        _components.view.filter { _.visible }.foreach { _.updateBounds() }
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
            else
            {
                // Includes half of the area between items (if there is no item, uses cap)
                val top = if (i > 0) (item.coordinateAlong(direction) - c(i - 1).maxCoordinateAlong(direction)) / 2 else
                    item.coordinateAlong(direction)
                val bottom = if (i < c.size - 1) (c(i + 1).coordinateAlong(direction) - item.maxCoordinateAlong(direction)) / 2 else
                    length - item.maxCoordinateAlong(direction)
                
                // Also includes the whole stack breadth
                Bounds(item.position - direction(top), item.size.withDimension(breadth, direction.perpendicular) +
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
        val p = relativePoint.along(direction)
        val c = components
        // Finds the first item past the relative point
        c.indexWhereOption { _.coordinateAlong(direction) > p }.map
        {
            nextIndex =>
                // Selects the next item if a) it's the first item or b) it's closer to point than the previous item
                if (nextIndex == 0 || c(nextIndex).coordinateAlong(direction) - p < p - c(nextIndex - 1).maxCoordinateAlong(direction))
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
    def indexOf(component: Any) = _components.indexWhereOption { _.source == component }
    
    /**
      * Replaces a component with a new version
      * @param oldComponent Old component
      * @param newComponent New component
      */
    def replace(oldComponent: Any, newComponent: C) = indexOf(oldComponent).foreach { insert(newComponent, _) }
    
    /**
      * Drops the last n items from this stack
      * @param amount The amount of items to remove from this stack
      */
    def dropLast(amount: Int) = _components dropRight amount map { _.source } foreach { -= }
}

private class StackItem[C <: Stackable](val source: C) extends Area with StackableWrapper
{
    // ATTRIBUTES    -------------------
    
    private var nextPosition: Option[Point] = None
    private var nextSize: Option[Size] = None
    
    
    // IMPLEMENTED    -----------------
    
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