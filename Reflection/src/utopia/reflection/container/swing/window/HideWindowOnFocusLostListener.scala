package utopia.reflection.container.swing.window

import java.awt.event.{WindowEvent, WindowFocusListener}

/**
  * This focus listener hides (without closing) a window when it loses focus
  * @author Mikko Hilpinen
  * @since 23.12.2020, v2
  */
class HideWindowOnFocusLostListener extends WindowFocusListener
{
	// ATTRIBUTES	--------------------------
	
	private var lastFocusWindow: Option[java.awt.Window] = None
	
	
	// IMPLEMENTED	--------------------------
	
	override def windowGainedFocus(e: WindowEvent) = lastFocusWindow = Some(e.getWindow)
	
	override def windowLostFocus(e: WindowEvent) =
	{
		lastFocusWindow.foreach { _.setVisible(false) }
		lastFocusWindow = None
	}
}
