package utopia.reflection.component.swing.template

import utopia.firmament.awt.AwtEventThread
import utopia.firmament.component.Component
import utopia.flow.view.mutable.caching.MutableLazy
import utopia.genesis.event.{MouseButtonStateEvent, MouseButtonStatus}
import utopia.genesis.graphics.FontMetricsWrapper
import utopia.genesis.handling.mutable._
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.reflection.component.template.layout.stack.{CachingReflectionStackable, ReflectionStackable, ReflectionStackLeaf}
import utopia.reflection.component.template.ReflectionComponentLike
import utopia.reflection.event.{ResizeEvent, ResizeListener}
import utopia.firmament.model.stack.StackSize
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.reflection.util.ComponentToImage

import java.awt.event.MouseEvent

object AwtComponentWrapper
{
    /**
     * Wraps a component
     */
    // TODO: Consider wrapping the whole hierarchy
    def apply(component: java.awt.Component): AwtComponentWrapper =
        new SimpleAwtComponentWrapper(component, Vector())
}

/**
* This class wraps a JComponent for a standardized interface
* @author Mikko Hilpinen
* @since 25.2.2019
**/
trait AwtComponentWrapper extends ReflectionComponentLike with AwtComponentRelated
{
    // ATTRIBUTES    ----------------------
    
    // Temporarily cached position and size
    private val cachedPosition = MutableLazy { Point(component.getX, component.getY) }
    private val cachedSize = MutableLazy { Size(component.getWidth, component.getHeight) }
    
    // Handlers for distributing events
    override val mouseButtonHandler = MouseButtonStateHandler()
    override val mouseMoveHandler = MouseMoveHandler()
    override val mouseWheelHandler = MouseWheelHandler()
    
    /**
     * The currently active resize listeners for this wrapper. Please note that the listeners
     * will by default only be informed on size changes made through this wrapper. Size changes
     * that happen directly in the component are ignored by default
     */
    override var resizeListeners = Vector[ResizeListener]()
    /**
     * Removes a resize listener from the informed listeners
     */
    def resizeListeners_-=(listener: Any) = resizeListeners = resizeListeners.filterNot { _ == listener }
    
    
    // COMPUTED ---------------------------
    
    /**
      * @return An image of this component's paint result. Please note that this draws the image with this component's
      *         current size. If this component doesn't have a size, no image is drawn.
      */
    def toImage = ComponentToImage(this)
    
    private def parentsInWindow = new ParentsIterator
    
    
    // IMPLEMENTED    ---------------------
    
    override def fontMetricsWith(font: Font): FontMetricsWrapper = component.getFontMetrics(font.toAwt)
    
    /**
      * @return This component's current position
      */
    override def position = cachedPosition.value
    override def position_=(p: Point) = {
        cachedPosition.value = p
        updateBounds()
    }
    
    /**
      * @return This component's current size
      */
    override def size = cachedSize.value
    override def size_=(s: Size) = {
        // Informs resize listeners, if there are any
        if (resizeListeners.isEmpty)
            cachedSize.value = s
        else {
            val oldSize = size
            cachedSize.value = s
            
            if (oldSize !~== s) {
                val newEvent = ResizeEvent(oldSize, s)
                resizeListeners.foreach { _.onResizeEvent(newEvent) }
            }
        }
        
        updateBounds()
    }
    
    /**
      * @return The parent component of this component (wrapped)
      */
    override def parent: Option[AwtComponentWrapper] =
        Option(component.getParent).map { new SimpleAwtComponentWrapper(_, Vector(this)) }
    
    /**
      * @return Whether this component is currently visible
      */
    override def visible = component.isVisible
    override def visible_=(isVisible: Boolean) = AwtEventThread.async { component.setVisible(isVisible) }
    
    /**
      * @return The background color of this component
      */
    override def background: Color = component.getBackground
    override def background_=(color: Color) = AwtEventThread.async {
        // Since swing components don't handle transparency very well, mixes a transparent color with background instead
        if (color.transparent)
        {
            val myRGB = color.rgb
            val parentRGB = parentBackground.map { _.rgb }
            
            if (parentRGB.isDefined)
            {
                // Picks average, weighting by alpha
                val finalRGB = myRGB * color.alpha + parentRGB.get * (1 - color.alpha)
                component.setBackground(finalRGB.toAwt)
            }
            else
                component.setBackground(myRGB.toAwt)
        }
        else
            component.setBackground(color.toAwt)
    }
    
    /**
      * @return Whether this component is transparent (not drawing full area)
      */
    override def isTransparent = !component.isOpaque
    
    // Absolute position needs to be calculated separately since parent might wrap multiple windows
    override def absolutePosition = parentsInWindow.foldLeft(position) { _ + _.position }
    
    
    // OTHER    -------------------------
    
    /**
      * @param imageSize Size of the resulting image
      * @return An image with this component painted on it
      */
    def toImageWithSize(imageSize: Size) = ComponentToImage(this, imageSize)
    
    /**
      * Enables awt event-conversion for received mouse button events in this component. Use this when the component
      * naturally consumes awt mouse button events and when you wish to share those events with the mouse event
      * handling system in Reflection.
      */
    def enableAwtMouseButtonEventConversion() = AwtEventThread.async {
        component.addMouseListener(new AwtMouseEventImporter)
    }
    
    /**
     * Transforms this wrapper into a Stackable
     */
    def withStackSize(getSize: () => StackSize, update: () => Unit = () => ()): AwtComponentWrapperWrapper with ReflectionStackable =
        new AwtComponentWrapperWrapperWithStackable(this, getSize, update)
    
    /**
     * Transforms this wrapper into a Stackable
     */
    def withStackSize(size: StackSize): AwtComponentWrapperWrapper with ReflectionStackable = withStackSize(() => size)
    
    private def updateBounds(): Unit = AwtEventThread.async {
        // Updates own position and size
        cachedPosition.current.foreach { p => component.setLocation(p.toAwtPoint) }
        cachedSize.current.foreach { s => component.setSize(s.toDimension) }
    }
    
    
    // NESTED   -------------------------
    
    // This iterator is used for iterating through parent components (bottom to top)
    private class ParentsIterator extends Iterator[AwtComponentWrapper]
    {
        private var nextParent = parent
        
        def hasNext = nextParent.isDefined
        
        def next() =
        {
            val result = nextParent.get
            // Will stop iteration after reaching a window component
            if (!result.component.isInstanceOf[java.awt.Window])
                nextParent = result.parent
            else
                nextParent = None
            result
        }
    }
    
    private class AwtMouseEventImporter extends java.awt.event.MouseListener
    {
        private var currentButtonStatus = MouseButtonStatus.empty
        
        override def mouseClicked(e: MouseEvent) = ()
        
        override def mousePressed(e: MouseEvent) = updateMouseButtonStatus(e, newStatus = true)
        
        override def mouseReleased(e: MouseEvent) = updateMouseButtonStatus(e, newStatus = false)
        
        override def mouseEntered(e: MouseEvent) = ()
        
        override def mouseExited(e: MouseEvent) = ()
        
        private def updateMouseButtonStatus(e: MouseEvent, newStatus: Boolean) =
        {
            currentButtonStatus += (e.getButton, newStatus)
            val eventPosition = positionOfEvent(e)
            val event = MouseButtonStateEvent(e.getButton, isDown = newStatus, eventPosition,
                absolutePosition + eventPosition, currentButtonStatus)
            distributeMouseButtonEvent(event)
        }
        
        private def positionOfEvent(e: MouseEvent) = Point.of(e.getPoint) + position
    }
}

private class SimpleAwtComponentWrapper(val component: java.awt.Component, override val children: Seq[Component])
    extends AwtComponentWrapper

private class AwtComponentWrapperWrapperWithStackable(override val wrapped: AwtComponentWrapper, getSize: () => StackSize, update: () => Unit)
    extends CachingReflectionStackable with AwtComponentWrapperWrapper with ReflectionStackLeaf
{
    // IMPLEMENTED  ---------------------
    
    override def calculatedStackSize = getSize()
    
    override protected def updateVisibility(visible: Boolean) = super[AwtComponentWrapperWrapper].visible_=(visible)
    
    override def visible_=(isVisible: Boolean) = super[CachingReflectionStackable].visible_=(isVisible)
    
    override def updateLayout() = update()
    
    override def children = super[CachingReflectionStackable].children
}