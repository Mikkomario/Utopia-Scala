package utopia.firmament.context
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.{StackInsets, StackLength}

/**
  * A common trait for contexts that wrap a text context
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
trait TextContextWrapper[+Repr] extends TextContextLike[Repr]
{
	// ABSTRACT	----------------------------
	
	override def wrapped: TextContext
	
	/**
	  * @param base A new text context to wrap
	  * @return A copy of this wrapper with that text context
	  */
	def withTextBase(base: TextContext): Repr
	
	
	// IMPLEMENTED	------------------------
	
	override def textAlignment = wrapped.textAlignment
	override def textInsets = wrapped.textInsets
	
	override def promptFont = wrapped.promptFont
	
	override def betweenLinesMargin = wrapped.betweenLinesMargin
	
	override def allowLineBreaks = wrapped.allowLineBreaks
	override def allowTextShrink = wrapped.allowTextShrink
	
	override def withDefaultPromptFont: Repr = withTextBase(wrapped.withDefaultPromptFont)
	override def withPromptFont(font: Font): Repr = withTextBase(wrapped.withPromptFont(font))
	override def withTextAlignment(alignment: Alignment): Repr = withTextBase(wrapped.withTextAlignment(alignment))
	override def withTextInsets(insets: StackInsets): Repr = withTextBase(wrapped.withTextInsets(insets))
	override def withMarginBetweenLines(margin: StackLength): Repr = withTextBase(wrapped.withMarginBetweenLines(margin))
	override def withAllowLineBreaks(allowLineBreaks: Boolean): Repr =
		withTextBase(wrapped.withAllowLineBreaks(allowLineBreaks))
	override def withAllowTextShrink(allowTextShrink: Boolean): Repr =
		withTextBase(wrapped.withAllowTextShrink(allowTextShrink))
	
	override def withColorBase(base: ColorContext): Repr = withTextBase(wrapped.withColorBase(base))
}
