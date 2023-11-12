package utopia.reflection.container.stack.template.layout

import utopia.firmament.component.{AreaOfItems, HasMutableBounds}
import utopia.firmament.component.stack.StackSizeCalculating
import utopia.firmament.controller.Stacker
import utopia.firmament.model.enumeration.StackLayout
import utopia.flow.collection.CollectionExtensions._
import utopia.paradigm.enumeration.Axis2D
import utopia.reflection.component.template.layout.stack.{ReflectionStackable, ReflectionStackableWrapper}
import utopia.reflection.container.stack.template.MultiStackContainer
import utopia.firmament.model.stack.StackLength
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
* A stack holds multiple stackable components in a stack-like manner either horizontally or vertically
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait ReflectionStackLike[C <: ReflectionStackable]
    extends MultiStackContainer[C] with StackSizeCalculating with AreaOfItems[C]
{
	// ATTRIBUTES    --------------------
    
    // FIXME: What is a variable doing in a trait?
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
    
    /**
      * Adds a component to the wrapped panel or other container
      * @param component Component to add
      * @param index Index where the component should be placed
      */
    protected def addToWrapped(component: C, index: Int): Unit
    /**
      * Removes a component from the wrapped panel or other container
      * @param component Component to remove
      */
    protected def removeFromWrapped(component: C): Unit
    
    
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
    
    override def components = _components map { _.source }
    
    override protected def addToContainer(component: C, index: Int): Unit = {
        _components = _components.inserted(new StackItem[C](component), index)
        addToWrapped(component, index)
    }
    override protected def removeFromContainer(component: C): Unit = {
        _components = _components.filterNot { _.source == component }
        removeFromWrapped(component)
    }
    
    def calculatedStackSize = Stacker.calculateStackSize(
        _components.filter { _.visible }.map { _.stackSize }, direction, margin, cap, layout)
    
    def updateLayout() = {
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
    override def areaOf(item: C) = {
        // Caches components so that indexes won't change in between
        val c = components
        c.findIndexOf(item).map { i =>
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
        c.findIndexWhere { _.position(direction) > p }.map { nextIndex =>
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
    def indexOf(component: Any) = _components.findIndexWhere { _.source == component }
    
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

private class StackItem[C <: ReflectionStackable](val source: C) extends HasMutableBounds with ReflectionStackableWrapper
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