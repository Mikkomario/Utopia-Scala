package utopia.reflection.container.stack

import utopia.flow.util.CollectionExtensions._
import utopia.genesis.shape.Axis2D
import utopia.genesis.shape.shape2D.{Bounds, Point, Size}
import utopia.reflection.component.stack.{StackSizeCalculating, Stackable, StackableWrapper}
import utopia.reflection.component.Area
import utopia.reflection.shape.StackLength

/**
* A stack holds multiple stackable components in a stack-like manner either horizontally or vertically
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait StackLike[C <: Stackable] extends MultiStackContainer[C] with StackSizeCalculating
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
    
    override def +=(component: C) =
    {
        _components :+= new StackItem(component)
        super.+=(component)
    }
    
    override def -=(component: C) =
    {
        _components = _components.filterNot { _.source == component }
        super.-=(component)
    }
    
    def calculatedStackSize = Stacker.calculateStackSize(
        _components.filter { _.isVisible }.map { _.stackSize }, direction, margin, cap, layout)
    
    def updateLayout() =
    {
        // Positions the components using a stacker
        Stacker(_components, Bounds(Point.origin, size), stackLength.optimal, direction, margin, cap, layout)
            
        // Finally applies the changes
        _components.view.filter { _.isVisible }.foreach { _.updateBounds() }
    }
    
    
    // OTHER    -----------------------
    
    /**
      * Inserts an item at a specific index in this stack
      * @param component The new component to be added
      * @param index The index where the component will be added
      */
    def insert(component: C, index: Int) =
    {
        _components = (_components.take(index) :+ new StackItem(component)) ++ _components.drop(index)
        super.+=(component)
    }
    
    /**
      * Drops the last n items from this stack
      * @param amount The amount of items to remove from this stack
      */
    def dropLast(amount: Int) = _components dropRight amount map { _.source } foreach { -= }
    
    /**
      * Finds the area of a single element in this stack, including the area around the object
      * @param item An item in this stack
      * @return The bounds around the item. None if the item isn't in this stack
      */
    def areaOf(item: C) =
    {
        // Caches components so that indexes won't change in between
        val c = components
        c.optionIndexOf(item).map
        {
            i =>
                if (c.size == 1)
                    Bounds(Point.origin, size)
                else
                {
                    // Includes half of the area between items (if there is no item, uses cap)
                    val top = if (i > 0) (item.coordinateAlong(direction) - c(i - 1).maxCoordinateAlong(direction)) / 2 else
                        item.coordinateAlong(direction)
                    val bottom = if (i < c.size - 1) (c(i + 1).coordinateAlong(direction) - item.maxCoordinateAlong(direction)) / 2 else
                        length - item.maxCoordinateAlong(direction)
                
                    Bounds(item.position - (top, direction), item.size + (top + bottom, direction))
                }
        }
    }
    
    /**
      * Finds the item that's neares to a <b>relative</b> point
      * @param relativePoint A point relative to this Stack's position ((0, 0) = stack origin)
      * @return The component that's nearest to the provided point. None if this stack is empty
      */
    def itemNearestTo(relativePoint: Point) =
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