package utopia.reflection.container.swing.window

import utopia.flow.time.WaitUtils

import javax.swing.JFrame
import utopia.genesis.image.Image
import utopia.reflection.component.template.layout.stack.{StackLeaf, Stackable}
import utopia.reflection.component.swing.template.{AwtComponentWrapper, AwtComponentWrapperWrapper}
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.container.swing.window.WindowResizePolicy.{Program, User}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Center
import utopia.reflection.shape.stack.StackSize
import utopia.reflection.util.ComponentCreationDefaults

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
      * @param resizeAlignment Alignment used when repositioning this window when its size changes
      *                        (used when program dictates window size). Default = Center = window's center point will
      *                        remain the same.
      * @param icon Icon to display on this window. Default = global default.
      * @param borderless Whether borderless windowed mode should be used (default = false)
      * @return A new windowed frame
      */
    def windowed[C <: Stackable with AwtContainerRelated](content: C, title: LocalizedString  = LocalizedString.empty,
                                                          resizePolicy: WindowResizePolicy = WindowResizePolicy.User,
                                                          resizeAlignment: Alignment = Center,
                                                          icon: Image = ComponentCreationDefaults.windowIcon,
                                                          borderless: Boolean = false) =
        new Frame(content, title, resizePolicy, resizeAlignment, icon, borderless, false, false)
    
    /**
      * Creates a new full screen frame
      * @param content The frame contents
      * @param title The frame title (default = empty string)
      * @param icon Icon to display on this window. Default = global default.
      * @param showToolBar Whether tool bar (bottom) should be displayed (default = true)
      * @param resizeAlignment Alignment that determines window position when its size changes
      *                        (used if this window becomes non-fullscreen). Default = Center.
      * @return A new full screen frame
      */
    def fullScreen[C <: Stackable with AwtContainerRelated](content: C, title: LocalizedString  = LocalizedString.empty,
                                                            icon: Image = ComponentCreationDefaults.windowIcon,
                                                            showToolBar: Boolean = true,
                                                            resizeAlignment: Alignment = Center) =
        new Frame(content, title, WindowResizePolicy.Program, resizeAlignment, icon, true, true,
            showToolBar)
    
    /**
     * Creates an invisible, zero sized frame
     * @param title Title for the frame (default = empty)
     * @return A new frame
     */
    def invisible(title: LocalizedString = LocalizedString.empty): Frame[Stackable with AwtContainerRelated] =
        new Frame(new ZeroSizePanel, title, Program, borderless = true)
    
    
    // NESTED   --------------------------------
    
    private class ZeroSizePanel extends AwtComponentWrapperWrapper with Stackable with StackLeaf with AwtContainerRelated
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
class Frame[C <: Stackable with AwtContainerRelated](override val content: C,
                                                     override val title: LocalizedString = LocalizedString.empty,
                                                     startResizePolicy: WindowResizePolicy = User,
                                                     override val resizeAlignment: Alignment = Center,
                                                     icon: Image = ComponentCreationDefaults.windowIcon,
                                                     val borderless: Boolean = false,
                                                     startFullScreen: Boolean = false,
                                                     startWithToolBar: Boolean = true) extends Window[C]
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
    def fullScreen_=(newStatus: Boolean) =
    {
        if (_fullScreen != newStatus)
        {
            _fullScreen = newStatus
            // Resizes and repositions the frame when status changes
            resetCachedSize()
            updateWindowBounds(true)
        }
    }
     
    def showsToolBar: Boolean = _showsToolBar
    def showsToolBar_=(newStatus: Boolean) =
    {
        if (_showsToolBar != newStatus)
        {
            _showsToolBar = newStatus
            // Resizes and repositions when status changes
            resetCachedSize()
            updateWindowBounds(true)
        }
    }
    
    def resizePolicy = _resizePolicy
    def resizePolicy_=(newPolicy: WindowResizePolicy) =
    {
        _resizePolicy = newPolicy
        _component.setResizable(newPolicy.allowsUserResize)
    }
    
    
    // OTHER    ------------------------
    
    /**
     * Sets it so that JVM will exit once this frame closes
      * @param delay Delay after window closing before closing the JVM
      * @param exc Implicit execution context
     */
    def setToExitOnClose(delay: FiniteDuration = Duration.Zero)(implicit exc: ExecutionContext) =
        closeFuture.onComplete { _ =>
            if (delay > Duration.Zero)
                WaitUtils.delayed(delay) { System.exit(0) }
            else
                System.exit(0)
        }
}