package utopia.firmament.component.text

import utopia.firmament.model.TextDrawContext

/**
  * Common trait for items that have/wrap a text draw context
  * @author Mikko Hilpinen
  * @since 10.4.2023, Reflection v2.0
  */
trait HasTextDrawContext
{
	// ABSTRACT ----------------------
	
	/**
	  * @return Context for drawing the text within this component
	  */
	def textDrawContext: TextDrawContext
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return The insets around the text in this component
	  */
	def textInsets = textDrawContext.insets
	/**
	  * @return This component's text alignment
	  */
	def alignment = textDrawContext.alignment
	/**
	  * @return The font used in this component
	  */
	def font = textDrawContext.font
	/**
	  * @return The color of the text in this component
	  */
	def textColor = textDrawContext.color
	/**
	  * @return Width threshold used for automated line-splitting (optional)
	  */
	def lineSplitThreshold: Option[Double] = textDrawContext.lineSplitThreshold
}
