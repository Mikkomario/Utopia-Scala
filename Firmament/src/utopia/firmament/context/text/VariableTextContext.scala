package utopia.firmament.context.text

import utopia.firmament.context.color.{ColorContext2, VariableColorContext, VariableColorContextWrapper}
import utopia.firmament.model.stack.{StackInsets, StackLength}
import utopia.firmament.model.{Margins, TextDrawContext}
import utopia.flow.collection.immutable.caching.cache.WeakCache
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.view.immutable.View
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment

object VariableTextContext
{
	// ATTRIBUTES   -------------------------------
	
	private val defaultTextInsetsCache = WeakCache { margins: Margins =>
		StackInsets.symmetric(margins.aroundSmall, margins.aroundVerySmall)
	}
	
	// Weakly caches text draw context pointers
	// Keys (ordered) are:
	//      1. Font pointer
	//      2. Insets pointer
	//      3. Text color pointer
	//      4. Line split threshold pointer
	//      5. Alignment + between lines margin + allow line breaks
	private val textDrawContextPointerCache = WeakCache.weakKeys { fontP: Changing[Font] =>
		WeakCache.weakKeys { insetsP: Changing[StackInsets] =>
			WeakCache.weakKeys { colorP: Changing[Color] =>
				WeakCache.weakKeys { thresholdP: Option[Changing[Double]] =>
					WeakCache.weakValues[(Alignment, Double, Boolean), Changing[TextDrawContext]] { case (alignment, betweenLines, allowLineBreaks) =>
						colorP.mergeWith(Vector(insetsP, fontP) ++ thresholdP) { color: Color =>
							TextDrawContext(fontP.value, color, alignment, insetsP.value, thresholdP.map { _.value },
								betweenLines, allowLineBreaks)
						}
					}
				}
			}
		}
	}
	
	
	// OTHER    -----------------------------------
	
	def apply(base: ColorContext2, alignment: Alignment = Alignment.Left,
	          lineSplitThresholdPointer: Option[Changing[Double]] = None, allowLineBreaks: Boolean = true,
	          allowTextShrink: Boolean = false): VariableTextContext =
	{
		???
	}
	
	
	// NESTED   -----------------------------------
	
	// TODO: Continue implementation
	private case class _VariableTextContext(base: VariableColorContext, textAlignment: Alignment,
	                                        betweenLinesMargin: StackLength,
	                                        textInsetsPointer: Changing[StackInsets],
	                                        lineSplitThresholdPointer: Option[Changing[Double]],
	                                        lazyTextDrawContextPointer: View[Changing[TextDrawContext]],
	                                        customPromptFontPointer: Option[Changing[Font]],
	                                        allowLineBreaks: Boolean, allowTextShrink: Boolean,
	                                        textInsetsAreCustom: Boolean)
		extends VariableColorContextWrapper[VariableColorContext, VariableTextContext] with VariableTextContext
	{
		// IMPLEMENTED  ------------------------
		
		override def self: VariableTextContext = this
		
		override def promptFontPointer: Changing[Font] = customPromptFontPointer.getOrElse(fontPointer)
		override def textDrawContextPointer: Changing[TextDrawContext] = lazyTextDrawContextPointer.value
		
		override def withDefaultPromptFont: VariableTextContext =
			if (customPromptFontPointer.isEmpty) this else copy(customPromptFontPointer = None)
		
		override def withBase(base: VariableColorContext): VariableTextContext = copy(base = base)
		
		override def withPromptFontPointer(p: Changing[Font]): VariableTextContext =
			copy(customPromptFontPointer = Some(p))
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
		
		override def *(mod: Double): VariableTextContext = {
			if (mod ~== 1.0)
				this
			else {
				val newBase = base * mod
				val newBetweenLinesMargin = betweenLinesMargin * mod
				val newTextInsetsPointer = {
					if (textInsetsAreCustom)
						textInsetsPointer.map { _ * mod }
					else
						Fixed(defaultTextInsetsCache(newBase.margins))
				}
				val newLineThresholdPointer = lineSplitThresholdPointer.map { _.map { _ * mod } }
				copy(
					base = newBase,
					betweenLinesMargin = newBetweenLinesMargin,
					textInsetsPointer = newTextInsetsPointer,
					lineSplitThresholdPointer = newLineThresholdPointer,
					lazyTextDrawContextPointer = Lazy { textDrawContextPointerCache(newBase.fontPointer)(
						newTextInsetsPointer)(newBase.textColorPointer)(newLineThresholdPointer)(
						(textAlignment, newBetweenLinesMargin.optimal, allowLineBreaks)) },
					customPromptFontPointer = customPromptFontPointer.map { _.map { _ * mod } })
			}
		}
	}
}

/**
  * Common trait for pointer-based text context implementations.
  * Removes generic type parameter from [[VariableTextContextLike]].
  * @author Mikko Hilpinen
  * @since 05.10.2024, v1.3.2
  */
trait VariableTextContext extends VariableColorContext with VariableTextContextLike[VariableTextContext]
