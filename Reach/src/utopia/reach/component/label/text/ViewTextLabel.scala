package utopia.reach.component.label.text

import utopia.firmament.component.display.PoolWithPointer
import utopia.firmament.component.text.TextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.localization.{DisplayFunction, LocalizedString}
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.immutable.eventful.{AlwaysFalse, AlwaysTrue, Fixed}
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.text.Font
import utopia.paradigm.color.Color
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.VariableBackgroundRoleAssignableFactory
import utopia.reach.component.factory.{BackgroundAssignable, FromVariableContextComponentFactoryFactory, FromVariableContextFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.genesis.graphics.Priority

case class ContextualViewTextLabelFactory(parentHierarchy: ComponentHierarchy, contextPointer: Changing[TextContext],
                                          customDrawers: Seq[CustomDrawer] = Empty,
                                          isHintPointer: Changing[Boolean] = AlwaysFalse,
                                          drawBackground: Boolean = false)
	extends VariableBackgroundRoleAssignableFactory[TextContext, ContextualViewTextLabelFactory]
		with CustomDrawableFactory[ContextualViewTextLabelFactory]
{
	// COMPUTED ---------------------------------
	
	/**
	 * @return A copy of this factory without contextual information
	 */
	def withoutContext = ViewTextLabelFactory(parentHierarchy)
	
	/**
	  * @return Copy of this factory that creates hint labels
	  */
	def hint = withIsHintPointer(AlwaysTrue)
	
	private def stylePointer = contextPointer.mergeWith(isHintPointer) { (context, isHint) =>
		TextDrawContext.createContextual(isHint)(context)
	}
	
	
	// IMPLEMENTED	-----------------------------
	
	override def withContextPointer(p: Changing[TextContext]): ContextualViewTextLabelFactory = copy(contextPointer = p)
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualViewTextLabelFactory =
		copy(customDrawers = drawers)
	
	override protected def withVariableBackgroundContext(newContextPointer: Changing[TextContext],
	                                                     backgroundDrawer: CustomDrawer): ContextualViewTextLabelFactory =
		copy(contextPointer = newContextPointer, customDrawers = backgroundDrawer +: customDrawers,
			drawBackground = true)
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param isHintPointer A pointer that indicates whether hints (true) or normal text (false) is displayed
	  * @return Copy of this factory that uses the specified pointer
	  */
	def withIsHintPointer(isHintPointer: Changing[Boolean]) =
		copy(isHintPointer = isHintPointer)
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer that is reflected on this label
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw) = {
		val label = new ViewTextLabel[A](parentHierarchy, contentPointer, stylePointer,
			contextPointer.mapWhile(parentHierarchy.linkPointer) { _.allowTextShrink }, displayFunction, customDrawers)
		// If background drawing is enabled, repaints when the background color changes
		if (drawBackground)
			contextPointer.addContinuousListener { e =>
				if (e.values.isAsymmetricBy { _.background })
					label.repaint()
			}
		label
	}
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer to the text displayed on this label
	  * @return A new label
	  */
	def text(contentPointer: Changing[LocalizedString]) =
		apply[LocalizedString](contentPointer, DisplayFunction.identity)
	/**
	  * Creates a label that displays static text
	  * @param content Text to display on this label
	  * @return A new label
	  */
	def text(content: LocalizedString): ViewTextLabel[LocalizedString] = text(Fixed(content))
	@deprecated("Renamed to .text(Changing)", "v1.1")
	def forText(contentPointer: Changing[LocalizedString]) = text(contentPointer)
	
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
  * Used for constructing new view text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class ViewTextLabelFactory(parentHierarchy: ComponentHierarchy, customDrawers: Seq[CustomDrawer] = Empty,
                                allowsTextToShrink: Boolean = false)
	extends FromVariableContextFactory[TextContext, ContextualViewTextLabelFactory]
		with CustomDrawableFactory[ViewTextLabelFactory] with BackgroundAssignable[ViewTextLabelFactory]
{
	// COMPUTED --------------------------------
	
	/**
	  * @return Copy of this factory that allows text to shrink to conserve space
	  */
	def allowingTextToShrink = copy(allowsTextToShrink = true)
	
	
	// IMPLEMENTED	----------------------------
	
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ViewTextLabelFactory = copy(customDrawers = drawers)
	override def withBackground(background: Color): ViewTextLabelFactory =
		withCustomDrawer(BackgroundDrawer(background))
	
	override def withContextPointer(context: Changing[TextContext]): ContextualViewTextLabelFactory =
		ContextualViewTextLabelFactory(parentHierarchy, context, customDrawers)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new text label
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param stylePointer    A pointer to this label's styling information
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], stylePointer: Changing[TextDrawContext],
	             displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		new ViewTextLabel(parentHierarchy, contentPointer, stylePointer, Fixed(allowsTextToShrink), displayFunction,
			customDrawers)
	
	/**
	  * Creates a new text label
	  * @param contentPointer     Pointer that is reflected on this label
	  * @param font               Font used when drawing the text
	  * @param displayFunction    Function used when converting content to text (default = toString)
	  * @param textColor          Color used when drawing the text (default = standard black)
	  * @param alignment          Text alignment (default = left)
	  * @param insets             Insets around the text (default = any insets, preferring zero)
	  * @param lineSplitThreshold A width threshold after which lines are split (optional)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0)
	  * @param allowLineBreaks    Whether line breaks within the text should be respected and applied (default = true)
	  * @return A new label
	  */
	def withStaticStyle[A](contentPointer: Changing[A], font: Font,
	                       displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                       textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
	                       insets: StackInsets = StackInsets.any, lineSplitThreshold: Option[Double] = None,
	                       betweenLinesMargin: Double = 0.0, allowLineBreaks: Boolean = true) =
		apply(contentPointer,
			Fixed(TextDrawContext(font, textColor, alignment, insets, lineSplitThreshold, betweenLinesMargin,
				allowLineBreaks)),
			displayFunction)
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer to the text displayed on this label
	  * @param stylePointer   A pointer to this label's styling information
	  * @return A new label
	  */
	def text(contentPointer: Changing[LocalizedString], stylePointer: Changing[TextDrawContext]) =
		apply[LocalizedString](contentPointer, stylePointer, DisplayFunction.identity)
	@deprecated("Renamed to .text(...)", "v1.1")
	def forText(contentPointer: Changing[LocalizedString], stylePointer: Changing[TextDrawContext]) =
		text(contentPointer, stylePointer)
	
	/**
	  * Creates a new text label
	  * @param contentPointer     Pointer to the text displayed on this label
	  * @param font               Font used when drawing the text
	  * @param textColor          Color used when drawing the text (default = standard black)
	  * @param alignment          Text alignment (default = left)
	  * @param insets             Insets around the text (default = any insets, preferring zero)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0)
	  * @param allowLineBreaks    Whether line breaks within the text should be respected and applied (default = true)
	  * @return A new label
	  */
	// WET WET
	// TODO: Remove this factory or replace these params with a single TextDrawContext param, too much repetition here
	def textWithStaticStyle(contentPointer: Changing[LocalizedString], font: Font,
	                        textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
	                        insets: StackInsets = StackInsets.any, lineSplitThreshold: Option[Double] = None,
	                        betweenLinesMargin: Double = 0.0,
	                        allowLineBreaks: Boolean = true) =
		withStaticStyle[LocalizedString](contentPointer, font, DisplayFunction.identity, textColor, alignment, insets,
			lineSplitThreshold, betweenLinesMargin, allowLineBreaks)
	/**
	  * Creates a new text label
	  * @param contentPointer     Pointer to the text displayed on this label
	  * @param font               Font used when drawing the text
	  * @param textColor          Color used when drawing the text (default = standard black)
	  * @param alignment          Text alignment (default = left)
	  * @param insets             Insets around the text (default = any insets, preferring zero)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0)
	  * @param allowLineBreaks    Whether line breaks within the text should be respected and applied (default = true)
	  * @return A new label
	  */
	@deprecated("Renamed to .textWithStaticStyle", "v1.1")
	def forTextWithStaticStyle(contentPointer: Changing[LocalizedString], font: Font,
	                           textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
	                           insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
	                           allowLineBreaks: Boolean = true) =
		textWithStaticStyle(contentPointer, font, textColor, alignment, insets, None, betweenLinesMargin, allowLineBreaks)
}

object ViewTextLabel extends Cff[ViewTextLabelFactory]
	with FromVariableContextComponentFactoryFactory[TextContext, ContextualViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewTextLabelFactory(hierarchy)
	
	override def withContextPointer(hierarchy: ComponentHierarchy,
	                                context: Changing[TextContext]): ContextualViewTextLabelFactory =
		ContextualViewTextLabelFactory(hierarchy, context)
}

/**
  * A text label that displays contents of a changing item
  * @author Mikko Hilpinen
  * @since 17.10.2020, v0.1
  */
class ViewTextLabel[+A](override val parentHierarchy: ComponentHierarchy, override val contentPointer: Changing[A],
                        stylePointer: Changing[TextDrawContext], allowTextShrinkPointer: Changing[Boolean] = AlwaysFalse,
                        displayFunction: DisplayFunction[A] = DisplayFunction.raw,
                        additionalDrawers: Seq[CustomDrawer] = Empty)
	extends CustomDrawReachComponent with TextComponent with PoolWithPointer[A, Changing[A]]
{
	// ATTRIBUTE	-------------------------------------
	
	/**
	  * Pointer containing the current (measured) text
	  */
	val textPointer = contentPointer.mergeWithWhile(stylePointer, parentHierarchy.linkPointer) { (content, style) =>
		measure(displayFunction(content), style)
	}
	override val customDrawers =  additionalDrawers :+ TextViewDrawer(textPointer, stylePointer)
	
	
	// INITIAL CODE	-------------------------------------
	
	// Revalidates and/or repaints this component on all text changes
	textPointer.addListener { event =>
		if (event.equalsBy { _.size })
			repaint()
		else
			revalidate()
	}
	// Style changes (color & alignment) also trigger a revalidation / repaint
	stylePointer.addListenerWhile(parentHierarchy.linkPointer) { event =>
		if (event.equalsBy { _.color } || event.equalsBy { _.alignment })
			repaint(Priority.Low)
	}
	allowTextShrinkPointer.addListenerWhile(parentHierarchy.linkPointer) { _ => revalidate() }
	
	
	// IMPLEMENTED	-------------------------------------
	
	override def measuredText = textPointer.value
	override def textDrawContext = stylePointer.value
	override def allowTextShrink: Boolean = allowTextShrinkPointer.value
	
	override def updateLayout() = ()
}
