package utopia.reflection.component.template

import utopia.firmament.component.Component
import utopia.flow.collection.immutable.Empty
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reflection.event.ResizeListener

/**
* This trait describes basic component features without any implementation
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait ReflectionComponentLike extends Component
{
    // ABSTRACT    ------------------------
    
    def resizeListeners: Seq[ResizeListener]
    def resizeListeners_=(listeners: Seq[ResizeListener]): Unit
    
    /**
      * @return The parent component of this component
      */
    def parent: Option[ReflectionComponentLike]
    
    /**
      * @return Whether this component is currently visible
      */
    def visible: Boolean
    /**
      * Updates this component's visibility
      * @param isVisible Whether this component is currently visible
      */
    def visible_=(isVisible: Boolean): Unit
    
    /**
      * @return The background color of this component
      */
    def background: Color
    def background_=(color: Color): Unit
    
    /**
      * @return Whether this component is transparent (not drawing full area)
      */
    def isTransparent: Boolean
    
    /*
    def keyStateHandler: KeyStateHandler
    def keyTypedHandler: KeyTypedHandler
     */
    
    /*
      * @return The components under this component
      */
    /*
    override def children: Seq[ReflectionComponentLike] = Vector()
    */
    
    // COMPUTED    ---------------------------
    
    /**
      * @return Whether this component is currently hidden / invisible
      */
    def invisible = !visible
    def invisible_=(notVisible: Boolean) = visible = !notVisible
    
    /**
      * @return An iterator of this components parents
      */
    def parents: Iterator[ReflectionComponentLike] = new ParentsIterator()
    
    /**
      * @return The background color of this component's first non-transparent parent. None if this component doesn't
      *         have a non-transparent parent
      */
    def parentBackground = parents.find { !_.isTransparent }.map { _.background }
    
    /**
      * @return This component's "absolute" position. Ie. Position on the screen, provided that this component belongs to
      *         a window. If this component isn't connected to a window, a position in relation to topmost component
      *         is returned.
      */
    def absolutePosition: Point = parent.map { _.absolutePosition + position }.getOrElse(position)
    
    
    // IMPLEMENTED  ---------------------
    
    override def bounds = Bounds(position, size)
    override def bounds_=(b: Bounds) = {
        position = b.position
        size = b.size
    }
    
    
    // OTHER    -------------------------
    
    /*
      * Distributes a keyboard state event through this component's hierarchy. Should only be called for components
      * in the topmost window
      * @param event A keyboard state event
      */
    /*
    def distributeKeyStateEvent(event: KeyStateEvent): Unit =
    {
        keyStateHandler.onKeyState(event)
        children.foreach { _.distributeKeyStateEvent(event) }
    }*/
    
    /*
      * Distributes a key typed event through this component's hierarchy. Should only be called for components
      * in the topmost window
      * @param event A key typed event
      */
    /*
    def distributeKeyTypedEvent(event: KeyTypedEvent): Unit =
    {
        keyTypedHandler.onKeyTyped(event)
        children.foreach { _.distributeKeyTypedEvent(event) }
    }*/
    
    /*
      * Adds a new key state listener to this wrapper
      * @param listener A listener
      */
    // def addKeyStateListener(listener: KeyStateListener) = keyStateHandler += listener
    
    /*
      * Adds a new key typed listener to this wrapper
      * @param listener A listener
      */
    // def addKeyTypedListener(listener: KeyTypedListener) = keyTypedHandler += listener
    
    /*
      * Removes a listener from this wrapper
      * @param listener A listener to be removed
      */
        /*
    def removeListener(listener: Handleable) = forMeAndChildren
    {
        c =>
            c.mouseButtonHandler -= listener
            c.mouseMoveHandler -= listener
            c.mouseWheelHandler -= listener
            c.keyStateHandler -= listener
            c.keyTypedHandler -= listener
    }*/
    
    /**
      * Adds a resize listener to listen to this component
      * @param listener A resize listener
      */
    def addResizeListener(listener: ResizeListener) = resizeListeners :+= listener
    /**
      * Removes a resize listener from this component
      * @param listener A resize listener to be removed
      */
    def removeResizeListener(listener: Any) = resizeListeners = resizeListeners.filterNot { _ == listener }
    /**
     * Removes all resize listeners from this wrapper
     */
    def clearResizeListeners() = resizeListeners = Empty
    
    
    // NESTED CLASSES    ----------------
    
    // This iterator is used for iterating through parent components (bottom to top)
    private class ParentsIterator extends Iterator[ReflectionComponentLike]
    {
        var nextParent = parent
        
        def hasNext = nextParent.isDefined
        
        def next() =
        {
            val result = nextParent.get
            nextParent = result.parent
            result
        }
    }
}
