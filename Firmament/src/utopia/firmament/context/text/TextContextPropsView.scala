package utopia.firmament.context.text

import utopia.firmament.context.color.ColorContextPropsView
import utopia.firmament.context.text.TextContextPropsView.hintToTextDrawContextPointerCache
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.view.template.eventful.{Changing, Flag}
import utopia.genesis.text.Font
import utopia.paradigm.enumeration.{Alignment, Axis2D}
import utopia.paradigm.enumeration.Axis.{X, Y}

object TextContextPropsView
{
	// ATTRIBUTES   -----------------------
	
	// A 3 levels deep weak cache for generated text draw context pointers
	// The keys are:
	//      1) Is hint -flag
	//      2) Text draw context pointer
	//      3) Hint text draw context pointer
	// NB: It is only recommended to use this cache with mutating hint flags
	private val hintToTextDrawContextPointerCache = WeakCache.weakKeys { isHintFlag: Flag =>
		WeakCache.weakKeys { contextPointer: Changing[TextDrawContext] =>
			WeakCache { hintContextPointer: Changing[TextDrawContext] =>
				isHintFlag.flatMap { if (_) hintContextPointer else contextPointer }
			}
		}
	}
}

/**
  * Access to text context properties. Doesn't limit the implementation to either fixed or variable approach.
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.4
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
	
	/**
	  * @param hint Whether targeting a hint text
	  * @return Pointer that contains the applied text draw context for that purpose
	  */
	def textDrawContextPointerFor(hint: Boolean) =
		if (hint) hintTextDrawContextPointer else textDrawContextPointer
	/**
	  * @param isHintFlag A flag that Whether targeting a hint text (true) or normal text (false).
	  * @return Pointer that contains the applied text draw context for that purpose
	  */
	def textDrawContextPointerFor(isHintFlag: Flag): Changing[TextDrawContext] = isHintFlag.fixedValue match {
		case Some(fixedState) => textDrawContextPointerFor(fixedState)
		case None => hintToTextDrawContextPointerCache(isHintFlag)(textDrawContextPointer)(hintTextDrawContextPointer)
	}
}
