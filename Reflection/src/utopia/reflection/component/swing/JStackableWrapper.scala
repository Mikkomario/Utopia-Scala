package utopia.reflection.component.swing

import javax.swing.JComponent
import utopia.reflection.component.stack.StackLeaf
import utopia.reflection.shape.StackSize

object JStackableWrapper
{
    /**
     * Wraps a component as stackable
     * @param component wrapped component
     * @param getSize a function for retrieving component size
     */
    def apply(component: JComponent, getSize: () => StackSize, update: () => Unit = () => Unit): JStackableWrapper =
        new JStackWrapper(component, getSize, update)
    
    /**
     * Wraps a component as stackable
     * @param component wrapped component
     * @param size fixed component sizes
     */
    def apply(component: JComponent, size: StackSize): JStackableWrapper = apply(component, () => size)
}

/**
* This trait combines JWrapper and Stackable traits
* @author Mikko Hilpinen
* @since 27.3.2019
**/
trait JStackableWrapper extends StackableAwtComponentWrapper with JWrapper

private class JStackWrapper(val component: JComponent, val getSize: () => StackSize, val update: () => Unit)
    extends JStackableWrapper with StackLeaf
{
    def calculatedStackSize = getSize()
    
    override def updateLayout() = update()
    
    override def children = Vector()
}