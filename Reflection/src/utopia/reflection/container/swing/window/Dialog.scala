package utopia.reflection.container.swing.window

import javax.swing.JDialog
import utopia.reflection.component.stack.Stackable
import utopia.reflection.container.swing.AwtContainerRelated
import utopia.reflection.container.swing.window.WindowResizePolicy.User
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Center

/**
* A frame operates as the / a main window in an app
* @author Mikko Hilpinen
* @since 26.3.2019
**/
class Dialog[C <: Stackable with AwtContainerRelated](owner: java.awt.Window, override val content: C,
                                                      override val title: LocalizedString,
                                                      startResizePolicy: WindowResizePolicy = User,
                                                      override val resizeAlignment: Alignment = Center,
                                                      borderless: Boolean = false) extends Window[C]
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
        center()
    }
    
	// IMPLEMENTED    ------------------
    
    def component = _component
    
    def fullScreen = false
    def showsToolBar: Boolean = true
    
    def resizePolicy = _resizePolicy
    def resizePolicy_=(newPolicy: WindowResizePolicy) =
    {
        _resizePolicy = newPolicy
        _component.setResizable(newPolicy.allowsUserResize)
    }
}