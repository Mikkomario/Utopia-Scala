package utopia.reflection.container.swing.window

import java.awt.event.{ComponentAdapter, ComponentEvent, KeyEvent, WindowAdapter, WindowEvent}

import utopia.flow.async.{VolatileFlag, VolatileOption}
import utopia.flow.datastructure.mutable.Lazy
import utopia.genesis.color.Color
import utopia.genesis.handling.mutable.ActorHandler
import utopia.genesis.handling._
import utopia.genesis.shape.{Axis, Axis2D, Vector3D}
import utopia.genesis.shape.shape2D.{Point, Size}
import utopia.genesis.view.{ConvertingKeyListener, MouseEventGenerator}
import utopia.reflection.component.stack.{Constrainable, Stackable}
import utopia.reflection.component.swing.AwtComponentRelated
import utopia.reflection.component.swing.button.ButtonLike
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.event.ResizeListener
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.{Alignment, Insets, StackSizeModifier}
import utopia.reflection.util.Screen

import scala.concurrent.Promise

/**
* This is a common wrapper for all window implementations
* @author Mikko Hilpinen
* @since 25.3.2019
**/
trait Window[Content <: Stackable with AwtComponentRelated] extends Stackable with AwtContainerRelated with Constrainable
{
    // ATTRIBUTES   ----------------
    
    private var _isAttachedToMainHierarchy = false
    private var _constraints = Vector[StackSizeModifier]()
    
    private val cachedStackSize = Lazy { calculatedStackSizeWithConstraints }
    private val generatorActivated = new VolatileFlag()
    private val closePromise = Promise[Unit]()
    
    private val uponCloseAction = VolatileOption[() => Unit]()
    
    
	// ABSTRACT    -----------------
    
    override def component: java.awt.Window
    
    /**
      * @return The localized title of this window
      */
    def title: LocalizedString
    
    /**
     * The content displayed in this window
     */
    def content: Content
    
    /**
     * Whether the OS toolbar should be shown
     */
    def showsToolBar: Boolean
    
    /**
     * Whether this window is currently set to full screen mode
     */
    def fullScreen: Boolean
    
    /**
      * @return The current resize policy for this window
      */
    def resizePolicy: WindowResizePolicy
    
    /**
      * @return Alignment used for positioning this window when its size changes
      */
    def resizeAlignment: Alignment
    
    
    // COMPUTED    ----------------
    
    /**
     * The insets in around this screen
     */
    def insets = Insets of component.getInsets
    
    /**
      * @return A future of this window's closing
      */
    def closeFuture = closePromise.future
    
    /**
      * @return Whether this is the currently focused window
      */
    def isFocusedWindow = component.isFocused
    
    
    // IMPLEMENTED    --------------
    
    override def constraints = _constraints
    
    override def constraints_=(newConstraints: Vector[StackSizeModifier]) =
    {
        _constraints = newConstraints
        revalidate()
    }
    
    override def stackId = hashCode()
    
    override def isAttachedToMainHierarchy = _isAttachedToMainHierarchy
    
    override def isAttachedToMainHierarchy_=(newAttachmentStatus: Boolean) =
    {
        // Informs / affects the content of this window as well
        if (_isAttachedToMainHierarchy != newAttachmentStatus)
        {
            _isAttachedToMainHierarchy = newAttachmentStatus
            if (newAttachmentStatus)
                content.attachToStackHierarchyUnder(this)
            else
                content.isAttachedToMainHierarchy = false
        }
    }
    
    override def children = Vector(content)
    
    override def stackSize = cachedStackSize.get
    
    override def resetCachedSize() = cachedStackSize.reset()
    
    override def isVisible_=(isVisible: Boolean) = component.setVisible(isVisible)
    
    // Size and position are not cached since the user may resize and move this Window at will, breaking the cached size / position logic
    override def size = Size(component.getWidth, component.getHeight)
    override def size_=(newSize: Size) = component.setSize(newSize.toDimension)
    
    override def position = Point(component.getX, component.getY)
    override def position_=(newPosition: Point) = component.setLocation(newPosition.toAwtPoint)
    
    override def calculatedStackSize =
    {
        val maxSize =
        {
            if (showsToolBar)
                Screen.size - Screen.insetsAt(component.getGraphicsConfiguration).total
            else
                Screen.size
        }
        val normal = (content.stackSize + insets.total).limitedTo(maxSize)
        
        // If on full screen mode, tries to maximize screen size
        if (fullScreen)
            normal.withOptimal(normal.max getOrElse normal.optimal)
        else
            normal
    }
    
    // Each time (content) layout is updated, may resize this window
    override def updateLayout() = updateWindowBounds(resizePolicy.allowsProgramResize)
    
    override def resizeListeners = content.resizeListeners
    override def resizeListeners_=(listeners: Vector[ResizeListener]) = content.resizeListeners = listeners
    
    // Windows have no parents
    override def parent = None
    
    override def isVisible = component.isVisible
    
    override def background = content.background
    override def background_=(color: Color) = content.background = color
    
    override def isTransparent = content.isTransparent
    
    override def fontMetrics = content.fontMetrics
    
    override def mouseButtonHandler = content.mouseButtonHandler
    override def mouseMoveHandler = content.mouseMoveHandler
    override def mouseWheelHandler = content.mouseWheelHandler
    override def keyStateHandler = content.keyStateHandler
    override def keyTypedHandler = content.keyTypedHandler
    
    
    // OTHER    --------------------
    
    /**
     * Displays this window, making it visible
     */
    def display(gainFocus: Boolean = true) =
    {
        if (gainFocus)
            isVisible = true
        else
        {
            component.setFocusableWindowState(false)
            isVisible = true
            component.setFocusableWindowState(true)
        }
    }
    
    /**
      * Setups up basic functionality in window. Should be called after this window has been filled with content and packed.
      */
    protected def setup() =
    {
        // Sets position and size
        updateWindowBounds(true)
    
        if (!fullScreen)
            position = ((Screen.size - size) / 2).toVector.toPoint
    
        updateContentBounds()
    
        // Registers to update bounds on each size change
        activateResizeHandling()
    
        // Registers self (and content) into stack hierarchy management
        _isAttachedToMainHierarchy = true
        content.attachToStackHierarchyUnder(this)
        
        component.addWindowListener(CloseListener)
    }
    
	/**
      * Starts mouse event generation for this window
      * @param actorHandler An actorhandler that generates the necessary action events
      */
    def startEventGenerators(actorHandler: ActorHandler) =
    {
        generatorActivated.runAndSet
        {
			// Starts mouse listening
            val mouseButtonListener = MouseButtonStateListener() { e => content.distributeMouseButtonEvent(e); None }
            val mouseMovelistener = MouseMoveListener() { content.distributeMouseMoveEvent(_) }
            val mouseWheelListener = MouseWheelListener() { content.distributeMouseWheelEvent(_) }
            
            val mouseEventGenerator = new MouseEventGenerator(content.component, mouseMovelistener, mouseButtonListener,
                mouseWheelListener, () => 1.0)
            actorHandler += mouseEventGenerator
			
			// Starts key listening
			val keyStateListener = KeyStateListener() { content.distributeKeyStateEvent(_) }
			val keyTypedListener = KeyTypedListener { content.distributeKeyTypedEvent(_) }
    
            val keyEventGenerator = new ConvertingKeyListener(keyStateListener, keyTypedListener)
            keyEventGenerator.register()
            
            // Quits event listening once this window finally closes
            uponCloseAction.setOne(() =>
            {
                actorHandler -= mouseEventGenerator
                keyEventGenerator.unregister()
            })
        }
    }
    
    /**
      * Registers the buttons used in this window so that the default button will be triggered on enter
      * @param defaultButton The default button for this window
      * @param moreButtons More buttons for this window
      */
    def registerButtons(defaultButton: ButtonLike, moreButtons: ButtonLike*) =
        addKeyStateListener(DefaultButtonHandler(defaultButton, moreButtons: _*) { isFocusedWindow })
    
    /**
     * Makes it so that this window will close one escape is pressed
     */
    def setToCloseOnEsc() = addKeyStateListener(KeyStateListener.onKeyPressed(KeyEvent.VK_ESCAPE) { _ =>
        if (isFocusedWindow) close() })
    
    /**
      * Updates the bounds of this window's contents to match those of this window
      */
    protected def updateContentBounds() = content.size = size - insets.total
    
    /**
      * Starts following component resizes, updating content size on each resize
      */
    protected def activateResizeHandling() =
    {
        component.addComponentListener(new ComponentAdapter
        {
            // Resizes content each time this window is resized
            // TODO: This will not limit user's ability to resize window beyond minimum and maximum
            override def componentResized(e: ComponentEvent) = updateContentBounds()
        })
    }
    
    /**
      * Updates this window's bounds according to changes either outside or inside this window
      * @param dictateSize Whether this window should dictate the resulting size (Full screen windows always dictate their size)
      */
    protected def updateWindowBounds(dictateSize: Boolean) =
    {
        if (fullScreen)
        {
            // Full screen mode always dictates size & position
            if (showsToolBar)
                position = Screen.insetsAt(component.getGraphicsConfiguration).toPoint
            else
                position = Point.origin
            
            size = stackSize.optimal
        }
        else
        {
            // In windowed mode, either dictates the new size or just makes sure its within limits
            if (dictateSize)
            {
                val oldSize = size
                size = stackSize.optimal
                
                val increase = size - oldSize
                // Window movement is determined by resize alignment
                val movement = Axis2D.values.map { axis =>
                    val move = resizeAlignment.directionAlong(axis) match
                    {
                        case Some(moveDirection) => if (moveDirection.isPositiveDirection) increase.along(axis) else 0.0
                        case None => increase.along(axis) / 2.0
                    }
                    (axis: Axis) -> move
                }.toMap
                position = (position - Vector3D.of(movement)).positive
            }
            else
                checkWindowBounds()
        }
    }
    
    /**
      * Makes sure the window bounds are within stackSize limits
      */
    protected def checkWindowBounds() =
    {
        // Checks current bounds against allowed limits
        stackSize.maxWidth.filter { _ < width }.foreach { width = _ }
        stackSize.maxHeight.filter { _ < height }.foreach { height = _ }
        
        if (isUnderSized)
            size = size max stackSize.min
    }
    
    /**
      * Closes (disposes) this window
      */
    def close() = component.dispose()
    
    /**
     * Centers this window on the screen
     */
    def centerOnScreen() = centerOn(null)
    
    /**
     * Centers this window on the screen or on the parent component
     */
    // TODO: This method is redundant in Frame, which has no parent
    def center() = centerOn(component.getParent)
    
    private def centerOn(component: java.awt.Component) =
    {
        if (fullScreen)
        {
            if (showsToolBar)
                position = Screen.insetsAt(component.getGraphicsConfiguration).toPoint
            else
                position = Point.origin
        }
        else
            this.component.setLocationRelativeTo(component)
    }
    
    
    // NESTED   -------------------------
    
    private object CloseListener extends WindowAdapter
    {
        override def windowClosed(e: WindowEvent) = handleClosing()
        
        override def windowClosing(e: WindowEvent) = handleClosing()
    
        private def handleClosing() =
        {
            // Performs a closing action, if one is queued
            uponCloseAction.pop().foreach { _() }
            closePromise.trySuccess(Unit)
            
            // Removes this window from the stack hierarchy (may preserve a portion of the content by detaching it first)
            detachFromMainStackHierarchy()
        }
    }
}
