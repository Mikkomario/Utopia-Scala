package utopia.firmament.component.text

import utopia.firmament.model.TextDrawContext

/**
  * Common trait for components that present text and allow outside style modifications
  * @author Mikko Hilpinen
  * @since 10.12.2019, Reflection v1
  */
trait MutableStyleTextComponent extends TextComponent with HasMutableTextDrawContext
{
	// OTHER	--------------------------
	
	/**
	  * Modifies the drawing context used by this text component
	  * @param f New drawing context
	  */
	@deprecated("Renamed to mapTextDrawContext", "v2.0")
	def mapDrawContext(f: TextDrawContext => TextDrawContext) = textDrawContext = f(textDrawContext)
}
