package utopia.firmament.component.container.many

import utopia.firmament.component.DelayedBoundsUpdate
import utopia.firmament.component.stack.{StackSizeCalculating, Stackable}
import utopia.firmament.controller.Stacker
import utopia.firmament.model.enumeration.StackLayout
import utopia.firmament.model.stack.StackLength
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
* A stack holds multiple stackable components in a stack-like manner either horizontally or vertically
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait StackLike[+C <: Stackable] extends MultiContainer[C] with StackSizeCalculating with Stackable
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
    
    override def toString = direction match {
        case X => s"Row[$count]"
        case Y => s"Column[$count]"
    }
    
    override def calculatedStackSize =
        Stacker.calculateStackSize(components.map { _.stackSize }, direction, margin, cap, layout)
    
    override def updateLayout() = {
        // Wraps the components in order to delay changes
        val wrappedComponents = components.map { DelayedBoundsUpdate(_) }
        
        // Positions the components using a stacker
        Stacker(wrappedComponents, Bounds(Point.origin, size), stackLength.optimal, direction, margin, cap, layout)
            
        // Finally applies the changes
        wrappedComponents.foreach { _.apply() }
    }
    
    
    // OTHER    -----------------------
    
    /**
      * @param component A component within this stack
      * @return Current index of the component. None if there's no such component in this stack
      */
    def indexOf(component: Any) = components.indexOf(component)
}