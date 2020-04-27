package utopia.reflection.component.context

import utopia.reflection.color.ColorSet

object ButtonContext
{
	/**
	  * @param textContext Context used for handling text
	  * @return A new button context that uses primary color scheme color
	  */
	def forPrimaryColorButtons(textContext: TextContextLike) = apply(textContext)
	
	/**
	  * @param textContext Context used for handling text
	  * @return A new button context that uses secondary color scheme color
	  */
	def forSecondaryColorButtons(textContext: TextContextLike) = apply(textContext,
		Some(textContext.colorScheme.secondary))
	
	/**
	  * @param textContext Context used for handling text
	  * @param color Color that should be used in the buttons created in this context
	  * @return A new button context that uses the specified color
	  */
	def forCustomColorButtons(textContext: TextContext, color: ColorSet) = apply(textContext, Some(color))
}

/**
  * A component creation context for buttons
  * @author Mikko Hilpinen
  * @since 27.4.2020, v1.2
  */
case class ButtonContext(base: TextContextLike, buttonColorOverride: Option[ColorSet] = None,
						 borderWidthOverride: Option[Double] = None)
	extends ButtonContextLike with TextContextWrapper
{
	// IMPLEMENTED	---------------------------
	
	override def buttonColor = buttonColorOverride.getOrElse(colorScheme.primary).forBackground(containerBackground)
	
	override def borderWidth = borderWidthOverride.getOrElse(margins.verySmall)
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param borderWidth New border width
	  * @return A copy of this context with the specified border width
	  */
	def withBorderWidth(borderWidth: Double) = copy(borderWidthOverride = Some(borderWidth))
}
