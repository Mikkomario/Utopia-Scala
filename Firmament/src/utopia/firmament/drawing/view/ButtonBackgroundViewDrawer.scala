package utopia.firmament.drawing.view

import utopia.firmament.model.enumeration.GuiElementState.Activated
import utopia.firmament.model.{Border, GuiElementStatus}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.graphics.{DrawSettings, Drawer}
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.Bounds
import utopia.firmament.drawing.template.DrawLevel.Background
import utopia.firmament.drawing.template.{BorderDrawerLike, DrawLevel}

/**
  * Used for drawing button background and border, based on button state
  * @author Mikko Hilpinen
  * @since 24.10.2020, Reflection v2
  * @param baseColorPointer A pointer to the button's default color
  * @param statePointer A pointer to the button's state
  * @param borderWidth Width of the drawn border in pixels (default = 0 = Don't draw the border)
  * @param colorChangeIntensity The modifier applied to color change in state changes (default = 1.0)
  * @param borderColorIntensity Color variance modifier applied on the raised border (default = 1.0)
  * @param drawLevel Drawing level used when using this drawer (default = Background)
  */
case class ButtonBackgroundViewDrawer(baseColorPointer: Changing[Color], statePointer: Changing[GuiElementStatus],
                                      borderWidth: Double = 0.0, colorChangeIntensity: Double = 1.0,
                                      borderColorIntensity: Double = 1.0, override val drawLevel: DrawLevel = Background)
	extends BorderDrawerLike
{
	// ATTRIBUTES	------------------------------
	
	private val drawsBorder = borderWidth > 0
	private val colorPointer = baseColorPointer.mergeWith(statePointer) { (c, s) => s.modify(c, colorChangeIntensity) }
	// Applies lesser raising while the button is pressed
	private val borderPointer = colorPointer.lazyMap { c =>
		Border.raised(borderWidth, c,
			if (statePointer.value is Activated) borderColorIntensity * 0.25 else borderColorIntensity)
	}
	private val bgDrawSettingsPointer = colorPointer.lazyMap(DrawSettings.onlyFill)
	
	
	// IMPLEMENTED	------------------------------
	
	override def draw(drawer: Drawer, bounds: Bounds) = {
		if (bounds.size.isPositive) {
			// Draws the background, then the border
			val backgroundArea = drawer.clippingBounds match {
				case Some(clipArea) => bounds.overlapWith(clipArea)
				case None => Some(bounds)
			}
			backgroundArea.foreach { drawer.draw(_)(bgDrawSettingsPointer.value) }
			if (drawsBorder)
				super.draw(drawer, bounds)
		}
	}
	
	override def border = borderPointer.value
}
