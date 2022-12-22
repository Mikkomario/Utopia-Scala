package utopia.reflection.component.context

import utopia.flow.operator.ScopeUsable
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}

object ButtonContext
{
	/**
	  * @param textContext Context used for handling text
	  * @param role Role for the buttons in this context
	  * @param preferredShade Preferred button shade (default = standard shade)
	  * @return A new button context
	  */
	def forRole(textContext: TextContext, role: ColorRole, preferredShade: ColorShade = Standard): ButtonContext =
		apply(textContext, Some(textContext.color(role, preferredShade)))
	
	/**
	  * @param textContext Context used for handling text
	  * @return A new button context that uses primary color scheme color
	  */
	def forPrimaryColorButtons(textContext: TextContext) = apply(textContext)
	
	/**
	  * @param textContext Context used for handling text
	  * @return A new button context that uses secondary color scheme color
	  */
	def forSecondaryColorButtons(textContext: TextContext) = apply(textContext,
		Some(textContext.colorScheme.secondary.forBackground(textContext.containerBackground)))
	
	/**
	  * @param textContext Context used for handling text
	  * @param color Color that should be used in the buttons created in this context
	  * @return A new button context that uses the specified color
	  */
	def forCustomColorButtons(textContext: TextContext, color: ComponentColor) = apply(textContext, Some(color))
}

/**
  * A component creation context for buttons
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class ButtonContext(base: TextContext, buttonColorOverride: Option[ComponentColor] = None,
						 borderWidthOverride: Option[Double] = None)
	extends ButtonContextLike with TextContextWrapper with BackgroundSensitive[ButtonContext] with ScopeUsable[ButtonContext]
{
	// IMPLEMENTED	---------------------------
	
	override def self = this
	
	override def buttonColor = buttonColorOverride.getOrElse(colorScheme.primary.forBackground(containerBackground))
	
	override def borderWidth = borderWidthOverride.getOrElse(margins.verySmall)
	
	override def inContextWithBackground(color: ComponentColor) = copy(base = base.inContextWithBackground(color))
	
	override def textColor = buttonColor.defaultTextColor
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param borderWidth New border width
	  * @return A copy of this context with the specified border width
	  */
	def withBorderWidth(borderWidth: Double) = copy(borderWidthOverride = Some(borderWidth))
}
