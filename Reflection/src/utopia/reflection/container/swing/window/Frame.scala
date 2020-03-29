package utopia.reflection.container.swing.window

import javax.swing.{JFrame, WindowConstants}
import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Center

object Frame
{
    /**
      * Creates a new windowed frame
      * @param content The frame contents
      * @param title The frame title
      * @param resizePolicy The policy used about Frame resizing. By default, only the user may resize the Frame
      * @param resizeAlignment Alignment used when repositioning this window when its size changes
      *                        (used when program dictates window size). Default = Center = window's center point will
      *                        remain the same.
      * @param borderless Whether borderless windowed mode should be used (default = false)
      * @return A new windowed frame
      */
    def windowed[C <: Stackable with AwtContainerRelated](content: C, title: LocalizedString,
                                                          resizePolicy: WindowResizePolicy = WindowResizePolicy.User,
                                                          resizeAlignment: Alignment = Center,
                                                          borderless: Boolean = false) =
        new Frame(content, title, resizePolicy, resizeAlignment, borderless, false, false)
    
    /**
      * Creates a new full screen frame
      * @param content The frame contents
      * @param title The frame title
      * @param showToolBar Whether tool bar (bottom) should be displayed (default = true)
      * @param resizeAlignment Alignment that determines window position when its size changes
      *                        (used if this window becomes non-fullscreen). Default = Center.
      * @return A new full screen frame
      */
    def fullScreen[C <: Stackable with AwtContainerRelated](content: C, title: LocalizedString, showToolBar: Boolean = true,
                                                            resizeAlignment: Alignment = Center) =
        new Frame(content, title, WindowResizePolicy.Program, resizeAlignment, true, true,
            showToolBar)
}

/**
* A frame operates as the / a main window in an app
* @author Mikko Hilpinen
* @since 26.3.2019
**/
class Frame[C <: Stackable with AwtContainerRelated](override val content: C, override val title: LocalizedString,
                                                     startResizePolicy: WindowResizePolicy = User,
                                                     override val resizeAlignment: Alignment = Center,
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
     */
    def setToExitOnClose() = _component.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
}