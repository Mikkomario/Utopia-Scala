package utopia.reach.component.label.text

import utopia.flow.view.immutable.eventful.{AlwaysFalse, Fixed}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.component.display.PoolWithPointer
import utopia.firmament.model.TextDrawContext
import utopia.firmament.component.text.TextComponent
import utopia.firmament.context.TextContext
import utopia.paradigm.color.ColorLevel.Standard
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.paradigm.enumeration.Alignment
import utopia.reach.util.Priority
import utopia.firmament.model.stack.StackInsets

object ViewTextLabel extends ContextInsertableComponentFactoryFactory[TextContext, ViewTextLabelFactory,
	ContextualViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewTextLabelFactory(hierarchy)
}

/**
  * Used for constructing new view text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class ViewTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContext, ContextualViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: TextContext](context: N) =
		ContextualViewTextLabelFactory(this, context)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer that is reflected on this label
	  * @param stylePointer A pointer to this label's styling information
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], stylePointer: Changing[TextDrawContext],
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	             customDrawers: Seq[CustomDrawer] = Vector(), allowTextShrink: Boolean = false) =
		new ViewTextLabel(parentHierarchy, contentPointer, stylePointer, displayFunction, customDrawers,
			allowTextShrink)
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer that is reflected on this label
	  * @param font Font used when drawing the text
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param alignment Text alignment (default = left)
	  * @param insets Insets around the text (default = any insets, preferring zero)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0)
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def withStaticStyle[A](contentPointer: Changing[A], font: Font,
	                       displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                       textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
	                       insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
	                       customDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
	                       allowTextShrink: Boolean = false) =
		apply(contentPointer,
			Fixed(TextDrawContext(font, textColor, alignment, insets, betweenLinesMargin, allowLineBreaks)),
			displayFunction, customDrawers, allowTextShrink)
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer to the text displayed on this label
	  * @param stylePointer A pointer to this label's styling information
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def forText(contentPointer: Changing[LocalizedString], stylePointer: Changing[TextDrawContext],
	            customDrawers: Seq[CustomDrawer] = Vector(),
	            allowTextShrink: Boolean = false) =
		apply[LocalizedString](contentPointer, stylePointer, DisplayFunction.identity, customDrawers,
			allowTextShrink)
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer to the text displayed on this label
	  * @param font Font used when drawing the text
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param alignment Text alignment (default = left)
	  * @param insets Insets around the text (default = any insets, preferring zero)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def forTextWithStaticStyle(contentPointer: Changing[LocalizedString], font: Font,
	                           textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
	                           insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
	                           additionalDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
	                           allowTextShrink: Boolean = false) =
		withStaticStyle[LocalizedString](contentPointer, font, DisplayFunction.identity, textColor, alignment, insets,
			betweenLinesMargin, additionalDrawers, allowLineBreaks, allowTextShrink)
}

case class ContextualViewTextLabelFactory[+N <: TextContext]
(factory: ViewTextLabelFactory, override val context: N)
	extends ContextualComponentFactory[N, TextContext, ContextualViewTextLabelFactory]
{
	// COMPUTED ---------------------------------
	
	/**
	 * @return A copy of this factory without contextual information
	 */
	def withoutContext = factory
	
	
	// IMPLEMENTED	-----------------------------
	
	override def withContext[N2 <: TextContext](newContext: N2) =
		ContextualViewTextLabelFactory(factory, newContext)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer that is reflected on this label
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHintPointer A pointer that that contains true when this label should be considered a hint
	  *                      (drawn with lesser opacity). Default = always false.
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	             isHintPointer: Changing[Boolean] = AlwaysFalse,
	             customDrawers: Seq[CustomDrawer] = Vector()) =
	{
		val stylePointer = isHintPointer.map { isHint => TextDrawContext.createContextual(isHint)(context) }
		factory(contentPointer, stylePointer, displayFunction, customDrawers, context.allowTextShrink)
	}
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer to the text displayed on this label
	  * @param isHintPointer A pointer that that contains true when this label should be considered a hint
	  *                      (drawn with lesser opacity). Default = always false.
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @return A new label
	  */
	def forText(contentPointer: Changing[LocalizedString], isHintPointer: Changing[Boolean] = AlwaysFalse,
	            customDrawers: Seq[CustomDrawer] = Vector()) =
		apply[LocalizedString](contentPointer, DisplayFunction.identity, isHintPointer, customDrawers)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param background      Label background color
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHintPointer   A pointer that that contains true when this label should be considered a hint
	  *                        (drawn with lesser opacity). Default = always false.
	  * @param customDrawers   Additional custom drawing (default = empty)
	  * @return A new label
	  */
	def withCustomBackground[A](contentPointer: Changing[A], background: Color,
	                            displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                            isHintPointer: Changing[Boolean] = AlwaysFalse,
	                            customDrawers: Seq[CustomDrawer] = Vector()) =
	{
		mapContext { _.against(background) }(contentPointer, displayFunction, isHintPointer,
			BackgroundDrawer(background) +: customDrawers)
	}
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param role            Label background color role
	  * @param preferredShade  Preferred color shade (default = standard)
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHintPointer   A pointer that that contains true when this label should be considered a hint
	  *                        (drawn with lesser opacity). Default = always false.
	  * @param customDrawers   Additional custom drawing (default = empty)
	  * @return A new label
	  */
	def withBackground[A](contentPointer: Changing[A], role: ColorRole,
	                      displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                      preferredShade: ColorLevel = Standard,
	                      isHintPointer: Changing[Boolean] = AlwaysFalse,
	                      customDrawers: Seq[CustomDrawer] = Vector()) =
		withCustomBackground(contentPointer, context.color.preferring(preferredShade)(role), displayFunction,
			isHintPointer, customDrawers)
}

/**
  * A text label that displays contents of a changing item
  * @author Mikko Hilpinen
  * @since 17.10.2020, v0.1
  */
class ViewTextLabel[+A](override val parentHierarchy: ComponentHierarchy, override val contentPointer: Changing[A],
                        stylePointer: Changing[TextDrawContext], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                        additionalDrawers: Seq[CustomDrawer] = Vector(),
                        override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with TextComponent with PoolWithPointer[A, Changing[A]]
{
	// ATTRIBUTE	-------------------------------------
	
	/**
	  * Pointer containing the current (measured) text
	  */
	val textPointer = contentPointer.mergeWith(stylePointer) { (content, style) =>
		measure(displayFunction(content), style)
	}
	override val customDrawers =  additionalDrawers.toVector :+ TextViewDrawer(textPointer, stylePointer)
	
	
	// INITIAL CODE	-------------------------------------
	
	// Revalidates and/or repaints this component on all text changes
	textPointer.addListener { event =>
		if (event.equalsBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	// Style changes (color & alignment) also trigger a revalidation / repaint
	stylePointer.addListener { event =>
		if (event.equalsBy { _.color } || event.equalsBy { _.alignment })
			repaint(Priority.Low)
	}
	
	
	// IMPLEMENTED	-------------------------------------
	
	override def measuredText = textPointer.value
	
	override def textDrawContext = stylePointer.value
	
	override def updateLayout() = ()
}
