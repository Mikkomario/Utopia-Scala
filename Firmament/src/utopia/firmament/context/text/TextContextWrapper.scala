package utopia.firmament.context.text

import utopia.firmament.context.color.ColorContextWrapper
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment

/**
  * Common trait for contexts which implement text context by wrapping such an instance
  * @tparam Base Wrapped context type
  * @tparam Repr Implementing type
  * @author Mikko Hilpinen
  * @since 09.10.2024, v1.4
  */
trait TextContextWrapper[Base <: TextContextCopyable[Base], +Repr]
	extends ColorContextWrapper[Base, Repr] with TextContextCopyable[Repr]
{
	override def textAlignment: Alignment = base.textAlignment
	override def betweenLinesMargin: StackLength = base.betweenLinesMargin
	override def allowLineBreaks: Boolean = base.allowLineBreaks
	override def allowTextShrink: Boolean = base.allowTextShrink
	override def promptFontPointer: Changing[Font] = base.promptFontPointer
	override def textInsetsPointer: Changing[StackInsets] = base.textInsetsPointer
	override def lineSplitThresholdPointer: Option[Changing[Double]] = base.lineSplitThresholdPointer
	override def textDrawContextPointer: Changing[TextDrawContext] = base.textDrawContextPointer
	override def hintTextDrawContextPointer: Changing[TextDrawContext] = base.hintTextDrawContextPointer
	
	override def withDefaultPromptFont: Repr = mapBase { _.withDefaultPromptFont }
	
	override def withPromptFont(font: Font): Repr = mapBase { _.withPromptFont(font) }
	override def withTextAlignment(alignment: Alignment): Repr = mapBase { _.withTextAlignment(alignment) }
	override def withTextInsets(insets: StackInsets): Repr = mapBase { _.withTextInsets(insets) }
	override def withLineSplitThreshold(threshold: Option[Double]): Repr =
		mapBase { _.withLineSplitThreshold(threshold) }
	override def withMarginBetweenLines(margin: StackLength): Repr = mapBase { _.withMarginBetweenLines(margin) }
	override def withAllowLineBreaks(allowLineBreaks: Boolean): Repr = mapBase { _.withAllowLineBreaks(allowLineBreaks) }
	override def withAllowTextShrink(allowTextShrink: Boolean): Repr = mapBase { _.withAllowTextShrink(allowTextShrink) }
	
	override def mapTextInsets(f: StackInsets => StackInsets): Repr = mapBase { _.mapTextInsets(f) }
}
