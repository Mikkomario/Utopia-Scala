package utopia.reflection.container.swing.window

import utopia.paradigm.shape.shape2d.{Bounds, Point}
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.firmament.localization.LocalizedString

import javax.swing.JDialog

/**
* A frame operates as the / a main window in an app
* @author Mikko Hilpinen
* @since 26.3.2019
  * @param owner The window that will host this dialog
  * @param content The component placed within this window
  * @param title Title text displayed on this window (localized, default = empty = no title)
  * @param startResizePolicy Policy to apply to resizing (default = user may resize this window)
  * @param screenBorderMargin Margin to place between this window and the screen borders,
  *                           when there is space (default = 0 px)
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
  * @param borderless Whether OS window borders should not be used for this window (default = false)
**/
class Dialog[C <: ReflectionStackable with AwtContainerRelated](owner: java.awt.Window, override val content: C,
                                                       override val title: LocalizedString = LocalizedString.empty,
                                                       startResizePolicy: WindowResizePolicy = User,
                                                       override val screenBorderMargin: Double = 0.0,
                                                       getAnchor: Bounds => Point = _.center,
                                                       borderless: Boolean = false)
    extends Window[C]
{
    // ATTRIBUTES    -------------------
    
    private val _component = new JDialog(owner, title.string)
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
        centerOnParent()
    }
    
	// IMPLEMENTED    ------------------
    
    def component = _component
    
    def fullScreen = false
    def showsToolBar: Boolean = true
    
    def resizePolicy = _resizePolicy
    def resizePolicy_=(newPolicy: WindowResizePolicy) = {
        _resizePolicy = newPolicy
        _component.setResizable(newPolicy.allowsUserResize)
    }
    
    override def absoluteAnchorPosition = getAnchor(bounds)
}