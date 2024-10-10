package utopia.firmament.context.text

import utopia.firmament.context.color.ColorContextPropsView
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.{Alignment, Axis2D}
import utopia.paradigm.enumeration.Axis.{X, Y}

/**
  * Access to text context properties. Doesn't limit the implementation to either fixed or variable approach.
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.3.2
  */
trait TextContextPropsView extends ColorContextPropsView
{
	// ABSTRACT	--------------------------
	
	/**
	  * @return Used text alignment
	  */
	def textAlignment: Alignment
	/**
	  * @return Margin placed between lines of text
	  */
	def betweenLinesMargin: StackLength
	/**
	  * @return Whether text display components should by default respect line breaks inside the displayed text,
	  *         drawing possibly multiple separate lines of text. If false, components should, by default, ignore
	  *         line breaks.
	  */
	def allowLineBreaks: Boolean
	/**
	  * @return Whether displayed text should be shrunk to conserve space when that seems necessary
	  */
	def allowTextShrink: Boolean
	
	/**
	  * @return A pointer that contains the font used in prompts & hints
	  */
	def promptFontPointer: Changing[Font]
	/**
	  * @return Pointer containing the insets / margins placed around drawn text
	  */
	def textInsetsPointer: Changing[StackInsets]
	/**
	  * @return A pointer that contains a width threshold after which text should be split into separate lines.
	  *         None if no automatic line-splitting should occur.
	  */
	def lineSplitThresholdPointer: Option[Changing[Double]]
	
	/**
	  * @return A pointer that contains the currently defined text draw context
	  */
	def textDrawContextPointer: Changing[TextDrawContext]
	/**
	  * @return A pointer that contains the currently defined text draw context,
	  *         applicable for hints.
	  */
	def hintTextDrawContextPointer: Changing[TextDrawContext]
	
	
	// COMPUTED -------------------------------
	
	/**
	  * @return Horizontal text alignment applied in this context
	  */
	def horizontalTextAlignment = textAlignmentAlong(X)
	/**
	  * @return Vertical text alignment applied in this context
	  */
	def verticalTextAlignment = textAlignmentAlong(Y)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return Text alignment used along that axis
	  */
	def textAlignmentAlong(axis: Axis2D) = textAlignment(axis)
}
