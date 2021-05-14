package utopia.reach.component.label.text

import utopia.genesis.color.Color
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext, TextDrawer2}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.template.text.TextComponent2
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText}

object TextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike, TextLabelFactory,
	ContextualTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = TextLabelFactory(hierarchy)
}

/**
  * Used for constructing new static text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class TextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext[C2 <: TextContextLike](context: C2) =
		ContextualTextLabelFactory(this, context)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new text label
	  * @param text Text displayed on this label
	  * @param font Font used when drawing the text
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param alignment Text alignment (default = left)
	  * @param insets Insets around the text (default = any insets, preferring zero)
	  * @param betweenLinesMargin Margin placed between lines of text when line breaks are used (default = 0)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks in the text should be recognized and respected (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply(text: LocalizedString, font: Font, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Left, insets: StackInsets = StackInsets.any,
			  betweenLinesMargin: Double = 0.0, additionalDrawers: Seq[CustomDrawer] = Vector(),
			  allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		new TextLabel(parentHierarchy, text, TextDrawContext(font, textColor, alignment, insets, betweenLinesMargin),
			additionalDrawers, allowLineBreaks, allowTextShrink)
}

object ContextualTextLabelFactory
{
	// EXTENSIONS	-----------------------------
	
	implicit class ColorChangingStaticTextLabelFactory[N <: TextContextLike with BackgroundSensitive[TextContextLike]]
	(val f: ContextualTextLabelFactory[N]) extends AnyVal
	{
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param text Text displayed on this label
		  * @param background Label background color
		  * @param additionalDrawers Additional custom drawing (default = empty)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withCustomBackground(text: LocalizedString, background: ComponentColor,
								 additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		{
			f.mapContext { _.inContextWithBackground(background) }(text,
				BackgroundDrawer(background) +: additionalDrawers, isHint)
		}
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param text Text displayed on this label
		  * @param role Label background color role
		  * @param preferredShade Preferred color shade (default = standard)
		  * @param additionalDrawers Additional custom drawing (default = empty)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withBackground(text: LocalizedString, role: ColorRole, preferredShade: ColorShade = Standard,
						   additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
			withCustomBackground(text, f.context.color(role, preferredShade), additionalDrawers, isHint)
	}
}

case class ContextualTextLabelFactory[+N <: TextContextLike]
(factory: TextLabelFactory, override val context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualTextLabelFactory]
{
	// IMPLEMENTED	-----------------------------
	
	override def withContext[C2 <: TextContextLike](newContext: C2) =
		ContextualTextLabelFactory(factory, newContext)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def apply(text: LocalizedString, additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		factory(text, context.font, if (isHint) context.hintTextColor else context.textColor,
			context.textAlignment, context.textInsets, context.betweenLinesMargin.optimal, additionalDrawers,
			context.allowLineBreaks, context.allowTextShrink)
}

/**
  * This text label doesn't allow content or styling modifications from outside but presents static text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  * @param parentHierarchy This component's parent hierarchy
  * @param text Text displayed on this label
  * @param drawContext Styling settings for text drawing and layout
  * @param additionalDrawers Additional custom drawing (default = empty)
  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
  */
class TextLabel(override val parentHierarchy: ComponentHierarchy, override val text: LocalizedString,
				override val drawContext: TextDrawContext, additionalDrawers: Seq[CustomDrawer] = Vector(),
				allowLineBreaks: Boolean = true, override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with TextComponent2
{
	// ATTRIBUTES	-----------------------------
	
	val fontMetricsContext = FontMetricsContext(fontMetrics(drawContext.font), drawContext.betweenLinesMargin)
	override val measuredText = MeasuredText(text, fontMetricsContext, drawContext.alignment, allowLineBreaks)
	override val customDrawers = additionalDrawers.toVector :+
		TextDrawer2(measuredText, drawContext.font, drawContext.insets, drawContext.color)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def updateLayout() = ()
	
	override def measure(text: LocalizedString) = MeasuredText(text, fontMetricsContext, alignment, allowLineBreaks)
}
