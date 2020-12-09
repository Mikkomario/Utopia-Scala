package utopia.reflection.component.reach.label

import utopia.flow.event.{ChangingLike, Fixed}
import utopia.genesis.color.Color
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.TextViewDrawer2
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.CustomDrawReachComponent
import utopia.reflection.component.template.display.PoolWithPointer
import utopia.reflection.component.template.text.TextComponent2
import utopia.reflection.localization.{DisplayFunction, LocalizedString}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText}

object ViewTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike, ViewTextLabelFactory,
	ContextualViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = ViewTextLabelFactory(hierarchy)
}

/**
  * Used for constructing new view text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class ViewTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: TextContextLike](context: N) =
		ContextualViewTextLabelFactory(this, context)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer that is reflected on this label
	  * @param stylePointer A pointer to this label's styling information
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply[A](contentPointer: ChangingLike[A], stylePointer: ChangingLike[TextDrawContext],
				 displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 additionalDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
				 allowTextShrink: Boolean = false) =
		new ViewTextLabel(parentHierarchy, contentPointer, stylePointer, displayFunction, additionalDrawers,
			allowLineBreaks, allowTextShrink)
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer that is reflected on this label
	  * @param font Font used when drawing the text
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param alignment Text alignment (default = left)
	  * @param insets Insets around the text (default = any insets, preferring zero)
	  * @param betweenLinesMargin Margin placed between horizontal text lines, in case there are many (default = 0)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def withStaticStyle[A](contentPointer: ChangingLike[A], font: Font,
						   displayFunction: DisplayFunction[A] = DisplayFunction.raw,
						   textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						   insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
						   additionalDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
						   allowTextShrink: Boolean = false) =
		apply(contentPointer, Fixed(TextDrawContext(font, textColor, alignment, insets, betweenLinesMargin)),
			displayFunction, additionalDrawers, allowLineBreaks, allowTextShrink)
	
	/**
	  * Creates a new text label
	  * @param contentPointer Pointer to the text displayed on this label
	  * @param stylePointer A pointer to this label's styling information
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks within the text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def forText(contentPointer: ChangingLike[LocalizedString], stylePointer: ChangingLike[TextDrawContext],
				additionalDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
				allowTextShrink: Boolean = false) =
		apply[LocalizedString](contentPointer, stylePointer, DisplayFunction.identity, additionalDrawers,
			allowLineBreaks, allowTextShrink)
	
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
	def forTextWithStaticStyle(contentPointer: ChangingLike[LocalizedString], font: Font,
						   textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
						   insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
						   additionalDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
						   allowTextShrink: Boolean = false) =
		withStaticStyle[LocalizedString](contentPointer, font, DisplayFunction.identity, textColor, alignment, insets,
			betweenLinesMargin, additionalDrawers, allowLineBreaks, allowTextShrink)
}

object ContextualViewTextLabelFactory
{
	// EXTENSIONS	-----------------------------
	
	implicit class ColorChangingViewTextLabelFactory[N <: TextContextLike with BackgroundSensitive[TextContextLike]]
	(val f: ContextualViewTextLabelFactory[N]) extends AnyVal
	{
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param contentPointer Pointer that is reflected on this label
		  * @param background Label background color
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param isHintPointer A pointer that that contains true when this label should be considered a hint
		  *                      (drawn with lesser opacity). Default = always false.
		  * @param additionalDrawers Additional custom drawing (default = empty)
		  * @return A new label
		  */
		def withCustomBackground[A](contentPointer: ChangingLike[A], background: ComponentColor,
									displayFunction: DisplayFunction[A] = DisplayFunction.raw,
									isHintPointer: ChangingLike[Boolean] = Fixed(false),
									additionalDrawers: Seq[CustomDrawer] = Vector()) =
		{
			f.mapContext { _.inContextWithBackground(background) }(contentPointer, displayFunction, isHintPointer,
				BackgroundDrawer(background) +: additionalDrawers)
		}
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param contentPointer Pointer that is reflected on this label
		  * @param role Label background color role
		  * @param preferredShade Preferred color shade (default = standard)
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param isHintPointer A pointer that that contains true when this label should be considered a hint
		  *                      (drawn with lesser opacity). Default = always false.
		  * @param additionalDrawers Additional custom drawing (default = empty)
		  * @return A new label
		  */
		def withBackground[A](contentPointer: ChangingLike[A], role: ColorRole,
							  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							  preferredShade: ColorShade = Standard,
							  isHintPointer: ChangingLike[Boolean] = Fixed(false),
							  additionalDrawers: Seq[CustomDrawer] = Vector()) =
			withCustomBackground(contentPointer, f.context.color(role, preferredShade), displayFunction,
				isHintPointer, additionalDrawers)
	}
}

case class ContextualViewTextLabelFactory[+N <: TextContextLike]
(factory: ViewTextLabelFactory, override val context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualViewTextLabelFactory]
{
	// IMPLEMENTED	-----------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) =
		ContextualViewTextLabelFactory(factory, newContext)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer that is reflected on this label
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHintPointer A pointer that that contains true when this label should be considered a hint
	  *                      (drawn with lesser opacity). Default = always false.
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @return A new label
	  */
	def apply[A](contentPointer: ChangingLike[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 isHintPointer: ChangingLike[Boolean] = Fixed(false),
				 additionalDrawers: Seq[CustomDrawer] = Vector()) =
	{
		val stylePointer = isHintPointer.map { isHint => TextDrawContext(context.font,
			if (isHint) context.hintTextColor else context.textColor, context.textAlignment, context.textInsets,
			context.betweenLinesMargin.optimal) }
		factory(contentPointer, stylePointer, displayFunction, additionalDrawers, context.allowLineBreaks,
			context.allowTextShrink)
	}
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param contentPointer Pointer to the text displayed on this label
	  * @param isHintPointer A pointer that that contains true when this label should be considered a hint
	  *                      (drawn with lesser opacity). Default = always false.
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @return A new label
	  */
	def forText(contentPointer: ChangingLike[LocalizedString], isHintPointer: ChangingLike[Boolean] = Fixed(false),
				 additionalDrawers: Seq[CustomDrawer] = Vector()) =
		apply[LocalizedString](contentPointer, DisplayFunction.identity, isHintPointer, additionalDrawers)
}

/**
  * A text label that displays contents of a changing item
  * @author Mikko Hilpinen
  * @since 17.10.2020, v2
  */
class ViewTextLabel[A](override val parentHierarchy: ComponentHierarchy, override val contentPointer: ChangingLike[A],
					   stylePointer: ChangingLike[TextDrawContext], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					   additionalDrawers: Seq[CustomDrawer] = Vector(), allowLineBreaks: Boolean = true,
					   override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with TextComponent2 with PoolWithPointer[A, ChangingLike[A]]
{
	// ATTRIBUTE	-------------------------------------
	
	/**
	  * Pointer containing the current (measured) text
	  */
	val textPointer = contentPointer.mergeWith(stylePointer) { (content, style) => MeasuredText(displayFunction(content),
		FontMetricsContext(fontMetrics(style.font), style.betweenLinesMargin), style.alignment, allowLineBreaks) }
	override val customDrawers =  additionalDrawers.toVector :+ TextViewDrawer2(textPointer, stylePointer)
	
	
	// INITIAL CODE	-------------------------------------
	
	// Revalidates and repaints this component on all text changes
	textPointer.addListener { event =>
		if (event.compareBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	
	
	// IMPLEMENTED	-------------------------------------
	
	override def measuredText = textPointer.value
	
	override def drawContext = stylePointer.value
	
	override def updateLayout() = ()
}
