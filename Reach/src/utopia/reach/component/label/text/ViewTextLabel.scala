package utopia.reach.component.label.text

import utopia.firmament.component.display.PoolWithPointer
import utopia.firmament.component.text.TextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reach.drawing.Priority

object ViewTextLabel extends Cff[ViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewTextLabelFactory(hierarchy)
}

/**
  * Used for constructing new view text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class ViewTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext(context: TextContext) =
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

case class ContextualViewTextLabelFactory(factory: ViewTextLabelFactory, context: TextContext,
                                          isHintPointer: Changing[Boolean] = AlwaysFalse,
                                          customDrawers: Vector[CustomDrawer] = Vector(),
                                          customStylePointer: Option[Changing[TextDrawContext]] = None)
	extends TextContextualFactory[ContextualViewTextLabelFactory]
		with ContextualBackgroundAssignableFactory[TextContext, ContextualViewTextLabelFactory]
		with CustomDrawableFactory[ContextualViewTextLabelFactory]
{
	// COMPUTED ---------------------------------
	
	/**
	 * @return A copy of this factory without contextual information
	 */
	def withoutContext = factory
	
	/**
	  * @return Copy of this factory that creates hint labels
	  */
	def hint = withIsHintPointer(AlwaysTrue)
	
	private def stylePointer = customStylePointer
		.getOrElse { isHintPointer.map { isHint => TextDrawContext.createContextual(isHint)(context) } }
	
	
	// IMPLEMENTED	-----------------------------
	
	override def self: ContextualViewTextLabelFactory = this
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualViewTextLabelFactory =
		copy(customDrawers = drawers)
	
	override def withContext(newContext: TextContext) = ContextualViewTextLabelFactory(factory, newContext)
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param isHintPointer A pointer that indicates whether hints (true) or normal text (false) is displayed
	  * @return Copy of this factory that uses the specified pointer
	  */
	def withIsHintPointer(isHintPointer: Changing[Boolean]) =
		copy(isHintPointer = isHintPointer)
	
	/**
	  * @param stylePointer A new custom style pointer to use
	  * @return Copy of this factory with the specified custom style
	  */
	def withStylePointer(stylePointer: Changing[TextDrawContext]) =
		copy(customStylePointer = Some(stylePointer))
	/**
	  * @param f A mapping function for the style used
	  * @return Copy of this factory with mapped style. Please note that only the current contextual information is used.
	  */
	def withStyleModification(f: TextDrawContext => TextDrawContext) =
		withStylePointer(stylePointer.map(f))
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer that is reflected on this label
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		factory(contentPointer, stylePointer, displayFunction, customDrawers, context.allowTextShrink)
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer to the text displayed on this label
	  * @return A new label
	  */
	def forText(contentPointer: Changing[LocalizedString]) =
		apply[LocalizedString](contentPointer, DisplayFunction.identity)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param background      Label background color
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(Color).apply(...) instead", "v1.1")
	def withCustomBackground[A](contentPointer: Changing[A], background: Color,
	                            displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		withBackground(background)(contentPointer, displayFunction)
}

/**
  * A text label that displays contents of a changing item
  * @author Mikko Hilpinen
  * @since 17.10.2020, v0.1
  */
class ViewTextLabel[+A](override val parentHierarchy: ComponentHierarchy, override val contentPointer: Changing[A],
                        stylePointer: Changing[TextDrawContext],
                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
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
			revalidate()
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
