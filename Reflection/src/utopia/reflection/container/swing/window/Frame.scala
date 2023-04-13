package utopia.reflection.container.swing.window

import utopia.firmament.context.ComponentCreationDefaults
import utopia.flow.async.process
import utopia.flow.util.logging.{Logger, SysErrLogger}
import utopia.genesis.image.Image
import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reflection.component.swing.template.{AwtComponentWrapper, AwtComponentWrapperWrapper}
import utopia.reflection.component.template.layout.stack.{ReflectionStackLeaf, ReflectionStackable}
import utopia.firmament.model.enumeration.WindowResizePolicy.{Program, User}
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.enumeration.WindowResizePolicy
import utopia.firmament.model.stack.StackSize

import javax.swing.JFrame
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.{Duration, FiniteDuration}

object Frame
{
    // OTHER    ----------------------------------
    
    /**
      * Creates a new windowed frame
      * @param content The frame contents
      * @param title The frame title (default = empty string)
      * @param resizePolicy The policy used about Frame resizing. By default, only the user may resize the Frame
      * @param screenBorderMargin Minimum margin to place between the screen borders and this window
      *                           when there is space (default = 0 px)
      * @param icon Icon to display on this window. Default = global default.
      * @param getAnchor A function that returns the "anchor" position of this window.
      *                  Anchor is the point that remains the same whenever the size of this window changes.
      *                  E.g. if the anchor point remains located at the top left corner of this window, the window
      *                  will expand right and down when its size increases.
      *
      *                  This function accepts the current bounds of this window,
      *                  where (0,0) is located at the top left corner of the screen.
      *                  The anchor position must always be returned in this same (absolute) coordinate system.
      *
      *                  The default implementation will place the anchor at the center of this window,
      *                  meaning that this window will expand equally to right and left, top and bottom,
      *                  provided that there is space available.
      *
      * @param borderless Whether borderless windowed mode should be used (default = false)
      * @return A new windowed frame
      */
    def windowed[C <: ReflectionStackable with AwtContainerRelated](content: C, title: LocalizedString  = LocalizedString.empty,
                                                          resizePolicy: WindowResizePolicy = WindowResizePolicy.User,
                                                          screenBorderMargin: Double = 0.0,
                                                          icon: Image = ComponentCreationDefaults.windowIcon,
                                                          getAnchor: Bounds => Point = _.center,
                                                          borderless: Boolean = false) =
        new Frame(content, title, resizePolicy, screenBorderMargin, icon, getAnchor, borderless,
            startFullScreen = false, startWithToolBar = false)
    
    /**
      * Creates a new full screen frame
      * @param content The frame contents
      * @param title The frame title (default = empty string)
      * @param icon Icon to display on this window. Default = global default.
      * @param showToolBar Whether tool bar (bottom) should be displayed (default = true)
      * @return A new full screen frame
      */
    def fullScreen[C <: ReflectionStackable with AwtContainerRelated](content: C, title: LocalizedString  = LocalizedString.empty,
                                                            icon: Image = ComponentCreationDefaults.windowIcon,
                                                            showToolBar: Boolean = true) =
        new Frame(content, title, WindowResizePolicy.Program, icon = icon, getAnchor = _.topLeft, borderless = true,
            startFullScreen = true, startWithToolBar = showToolBar)
    
    /**
     * Creates an invisible, zero sized frame
     * @param title Title for the frame (default = empty)
     * @return A new frame
     */
    def invisible(title: LocalizedString = LocalizedString.empty): Frame[ReflectionStackable with AwtContainerRelated] =
        new Frame(new ZeroSizePanel, title, Program, borderless = true)
    
    
    // NESTED   --------------------------------
    
    private class ZeroSizePanel extends AwtComponentWrapperWrapper with ReflectionStackable with ReflectionStackLeaf with AwtContainerRelated
    {
        // ATTRIBUTES   ------------------------
        
        private val panel = new Panel[AwtComponentWrapper]
        
        
        // IMPLEMENTED  ------------------------
    
        override def component = panel.component
    
        override protected def wrapped = panel
    
        override def updateLayout() = ()
    
        override def stackSize = StackSize.fixedZero
    
        override def resetCachedSize() = ()
    
        override lazy val stackId = hashCode()
    }
}

/**
* A frame operates as the / a main window in an app
* @author Mikko Hilpinen
* @since 26.3.2019
**/
class Frame[C <: ReflectionStackable with AwtContainerRelated](override val content: C,
                                                               override val title: LocalizedString = LocalizedString.empty,
                                                               startResizePolicy: WindowResizePolicy = User,
                                                               override val screenBorderMargin: Double = 0.0,
                                                               icon: Image = ComponentCreationDefaults.windowIcon,
                                                               getAnchor: Bounds => Point = _.center,
                                                               val borderless: Boolean = false,
                                                               startFullScreen: Boolean = false,
                                                               startWithToolBar: Boolean = true)
    extends Window[C]
{
    // ATTRIBUTES    -------------------
    
    private val _component = new JFrame(title.string)
    
    private var _fullScreen = startFullScreen
    private var _showsToolBar = startWithToolBar
    private var _resizePolicy = startResizePolicy
    
    
    // INITIAL CODE    -----------------
    
    {
        // Makes sure content size has been cached (so that events will be fired correctly)
        content.size
        
        // Sets up the underlying frame
        _component.setContentPane(content.component)
        _component.setUndecorated(borderless)
        _component.setResizable(startResizePolicy.allowsUserResize)
        _component.pack()
    
        setup()
    }
    setIcon(icon)
    
    
	// IMPLEMENTED    ------------------
    
    def component: JFrame = _component
    
    def fullScreen: Boolean = _fullScreen
    def fullScreen_=(newStatus: Boolean) = {
        if (_fullScreen != newStatus) {
            _fullScreen = newStatus
            // Resizes and repositions the frame when status changes
            resetCachedSize()
            updateWindowBounds(true)
        }
    }
    
    def showsToolBar: Boolean = _showsToolBar
    def showsToolBar_=(newStatus: Boolean) = {
        if (_showsToolBar != newStatus) {
            _showsToolBar = newStatus
            // Resizes and repositions when status changes
            resetCachedSize()
            updateWindowBounds(true)
        }
    }
    
    def resizePolicy = _resizePolicy
    def resizePolicy_=(newPolicy: WindowResizePolicy) = {
        _resizePolicy = newPolicy
        _component.setResizable(newPolicy.allowsUserResize)
    }
    
    override def absoluteAnchorPosition = getAnchor(bounds)
    
    
    // OTHER    ------------------------
    
    /**
     * Sets it so that JVM will exit once this frame closes
      * @param delay Delay after window closing before closing the JVM
      * @param exc Implicit execution context
     */
    def setToExitOnClose(delay: FiniteDuration = Duration.Zero)(implicit exc: ExecutionContext) = {
        implicit val logger: Logger = SysErrLogger
        closeFuture.onComplete { _ => process.Delay(delay) { System.exit(0) } }
    }
}