package utopia.reflection.component.swing

import java.awt.event.ActionEvent

import javax.swing.{AbstractAction, JComponent, KeyStroke}
import utopia.genesis.color.Color
import utopia.reflection.shape.Border

/**
  * These items are related to a swing component
  * @author Mikko Hilpinen
  * @since 28.4.2019, v1+
  */
trait SwingComponentRelated extends AwtComponentRelated
{
	// ABSTRACT	--------------------------
	
	def component: JComponent
	
	
	// COMPUTED    -----------------------
	
	def isTransparent_=(isTransparent: Boolean) = component.setOpaque(!isTransparent)
	
	/**
	  * Changes the border of this component
	  * @param border A new border
	  */
	def setBorder(border: Border) = component.setBorder(border.toAwt)
	
	/**
	  * Removes any border from this component
	  */
	def clearBorder() = component.setBorder(null)
	
	/**
	  * Adds a keystroke shortcut to the wrapped component
	  * @param key the target key code
	  * @param action the action performed when the key is pressed
	  */
	def addShortcut(key: Int, action: () => Unit) =
	{
		val actionName = s"action-for-key-$key"
		component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key, 0), actionName)
		component.getActionMap.put(actionName, new RunAction(action))
	}
	
	private class RunAction(val action: () => Unit) extends AbstractAction
	{
		def actionPerformed(e: ActionEvent) = action()
	}
}
