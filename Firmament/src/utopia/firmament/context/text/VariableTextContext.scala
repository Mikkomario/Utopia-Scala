package utopia.firmament.context.text

import utopia.firmament.context.color.{VariableColorContext, VariableColorContextWrapper}
import utopia.firmament.model.{Margins, TextDrawContext}
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.Alignment

object VariableTextContext
{
	// ATTRIBUTES   -------------------------------
	
	private val defaultTextInsetsCache = WeakCache { margins: Margins =>
		StackInsets.symmetric(margins.aroundSmall, margins.aroundVerySmall)
	}
	
	
	// OTHER    -----------------------------------
	
	
	// NESTED   -----------------------------------
	
	// TODO: Continue implementation
	private case class _VariableTextContext(base: VariableColorContext, textAlignment: Alignment,
	                                        betweenLinesMargin: StackLength,
	                                        promptFontPointer: Changing[Font],
	                                        textInsetsPointer: Changing[StackInsets],
	                                        lineSplitThresholdPointer: Option[Changing[Double]],
	                                        allowLineBreaks: Boolean, allowTextShrink: Boolean,
	                                        textInsetsAreCustom: Boolean)
		extends VariableColorContextWrapper[VariableColorContext, VariableTextContext] with VariableTextContext
	{
		// ATTRIBUTES   ------------------------
		
		// TODO: Challenge here is how to implement weak caching
		override def textDrawContextPointer: Changing[TextDrawContext] = ???
		
		
		// IMPLEMENTED  ------------------------
		
		override def self: VariableTextContext = this
		
		override def withDefaultPromptFont: VariableTextContext = withPromptFontPointer(fontPointer)
		
		override def withBase(base: VariableColorContext): VariableTextContext = copy(base = base)
		
		override def withPromptFontPointer(p: Changing[Font]): VariableTextContext = copy(promptFontPointer = p)
		override def withTextInsetsPointer(p: Changing[StackInsets]): VariableTextContext =
			copy(textInsetsPointer = p, textInsetsAreCustom = true)
		override def withLineSplitThresholdPointer(p: Option[Changing[Double]]): VariableTextContext =
			copy(lineSplitThresholdPointer = p)
		
		override def withTextAlignment(alignment: Alignment): VariableTextContext = copy(textAlignment = alignment)
		override def withMarginBetweenLines(margin: StackLength): VariableTextContext = copy(betweenLinesMargin = margin)
		override def withAllowLineBreaks(allowLineBreaks: Boolean): VariableTextContext =
			copy(allowLineBreaks = allowLineBreaks)
		override def withAllowTextShrink(allowTextShrink: Boolean): VariableTextContext =
			copy(allowTextShrink = allowTextShrink)
		
		override def *(mod: Double): VariableTextContext = ???
	}
}

/**
  * Common trait for pointer-based text context implementations.
  * Removes generic type parameter from [[VariableTextContextLike]].
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.3.2
  */
trait VariableTextContext extends VariableColorContext with VariableTextContextLike[VariableTextContext]
