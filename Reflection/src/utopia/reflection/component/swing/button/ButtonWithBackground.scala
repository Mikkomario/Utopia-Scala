package utopia.reflection.component.swing.button

import utopia.firmament.drawing.mutable.MutableCustomDrawable
import utopia.firmament.drawing.view.BorderViewDrawer
import utopia.firmament.model.{Border, GuiElementStatus}
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.Color

/**
  * An abstract implementation common to buttons that have a solid background and a border
  * @author Mikko Hilpinen
  * @since 24.9.2020, v1.3
  */
abstract class ButtonWithBackground(color: Color, borderWidth: Double) extends ButtonLike with MutableCustomDrawable
{
	// ATTRIBUTES   -------------------
	
	private val borderPointer = EventfulPointer(makeBorder(color))
	
	
	// IMPLEMENTED  ------------------
	
	override protected def updateStyleForState(newState: GuiElementStatus) = {
		val newColor = newState.modify(color)
		background = newColor
		borderPointer.value = makeBorder(newColor)
	}
	
	
	// OTHER	----------------------
	
	/**
	  * Sets up custom drawing, component background and styling. Should be called from the sub-class once other
	  * initialization has completed.
	  * @param hotKeys Hotkeys that trigger this button (key codes) (default = empty)
	  * @param hotKeyChars Hotkey characters that trigger this button (default = empty)
	  */
	protected def setup(hotKeys: Set[Int] = Set(), hotKeyChars: Iterable[Char] = Set()) =
	{
		component.setFocusable(true)
		setHandCursor()
		background = color
		
		initializeListeners(hotKeys, hotKeyChars)
		
		// Adds border drawing
		if (borderWidth > 0) {
			addCustomDrawer(BorderViewDrawer(borderPointer))
			borderPointer.addContinuousAnyChangeListener { repaint() }
		}
	}
	
	private def makeBorder(baseColor: Color) = Border.raised(borderWidth, baseColor)
}
