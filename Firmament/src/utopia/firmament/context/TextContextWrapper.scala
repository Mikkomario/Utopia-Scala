package utopia.firmament.context
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment
import utopia.firmament.model.stack.{StackInsets, StackLength}

/**
  * A common trait for contexts that wrap a text context
  * @author Mikko Hilpinen
  * @since 27.4.2020, Reflection v1.2
  */
@deprecated("Replaced with a new version", "v1.4")
trait TextContextWrapper[+Repr] extends TextContextLike[Repr]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return Wrapped text context
	  */
	def textContext: TextContext
	
	/**
	  * @param base A new text context to wrap
	  * @return A copy of this wrapper with that text context
	  */
	def withTextContext(base: TextContext): Repr
	
	
	// IMPLEMENTED	------------------------
	
	override def colorContext: ColorContext = textContext
	
	override def textAlignment = textContext.textAlignment
	override def textInsets = textContext.textInsets
	
	override def promptFont = textContext.promptFont
	
	override def lineSplitThreshold: Option[Double] = textContext.lineSplitThreshold
	override def betweenLinesMargin = textContext.betweenLinesMargin
	
	override def allowLineBreaks = textContext.allowLineBreaks
	override def allowTextShrink = textContext.allowTextShrink
	
	override def withDefaultPromptFont: Repr = withTextContext(textContext.withDefaultPromptFont)
	override def withPromptFont(font: Font): Repr = withTextContext(textContext.withPromptFont(font))
	override def withTextAlignment(alignment: Alignment): Repr = withTextContext(textContext.withTextAlignment(alignment))
	override def withTextInsets(insets: StackInsets): Repr = withTextContext(textContext.withTextInsets(insets))
	override def withLineSplitThreshold(threshold: Option[Double]): Repr =
		withTextContext(textContext.withLineSplitThreshold(threshold))
	override def withMarginBetweenLines(margin: StackLength): Repr = withTextContext(textContext.withMarginBetweenLines(margin))
	override def withAllowLineBreaks(allowLineBreaks: Boolean): Repr =
		withTextContext(textContext.withAllowLineBreaks(allowLineBreaks))
	override def withAllowTextShrink(allowTextShrink: Boolean): Repr =
		withTextContext(textContext.withAllowTextShrink(allowTextShrink))
	
	override def withColorContext(base: ColorContext): Repr = withTextContext(textContext.withColorContext(base))
	
	
	// OTHER    ------------------------
	
	/**
	  * @param f A mapping function for the wrapped text context
	  * @return A copy of this context with mapped text context
	  */
	def mapTextContext(f: TextContext => TextContext) = withTextContext(f(textContext))
}
