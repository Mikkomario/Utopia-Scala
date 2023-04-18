package utopia.reach.component.label.text

import utopia.firmament.component.text.TextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, TextDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.{FromContextFactory, TextContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.CustomDrawReachComponent

object TextLabel extends Cff[TextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = TextLabelFactory(hierarchy)
}

/**
  * Used for constructing new static text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class TextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext(context: TextContext) =
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
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param allowLineBreaks Whether line breaks in the text should be recognized and respected (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply(text: LocalizedString, font: Font, textColor: Color = Color.textBlack,
	          alignment: Alignment = Alignment.Left, insets: StackInsets = StackInsets.any,
	          betweenLinesMargin: Double = 0.0, customDrawers: Seq[CustomDrawer] = Vector(),
	          allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		new TextLabel(parentHierarchy, text, TextDrawContext(font, textColor, alignment, insets,
			betweenLinesMargin, allowLineBreaks), customDrawers, allowTextShrink)
}

case class ContextualTextLabelFactory(factory: TextLabelFactory, context: TextContext)
	extends TextContextualFactory[ContextualTextLabelFactory]
{
	// IMPLEMENTED	-----------------------------
	
	override def self: ContextualTextLabelFactory = this
	
	override def withContext(newContext: TextContext) = ContextualTextLabelFactory(factory, newContext)
	
	
	// OTHER	---------------------------------
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def apply(text: LocalizedString, customDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		factory(text, context.font, if (isHint) context.hintTextColor else context.textColor,
			context.textAlignment, context.textInsets, context.betweenLinesMargin.optimal, customDrawers,
			context.allowLineBreaks, context.allowTextShrink)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text              Text displayed on this label
	  * @param background        Label background color
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param isHint            Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def withCustomBackground(text: LocalizedString, background: Color,
	                         customDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		mapContext { _.against(background) }(text,
			BackgroundDrawer(background) +: customDrawers, isHint)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text              Text displayed on this label
	  * @param role              Label background color role
	  * @param preferredShade    Preferred color shade (default = standard)
	  * @param customDrawers Additional custom drawing (default = empty)
	  * @param isHint            Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def withBackground(text: LocalizedString, role: ColorRole, preferredShade: ColorLevel = Standard,
	                   customDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		withCustomBackground(text, context.color.preferring(preferredShade)(role), customDrawers, isHint)
}

/**
  * This text label doesn't allow content or styling modifications from outside but presents static text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  * @param parentHierarchy This component's parent hierarchy
  * @param text Text displayed on this label
  * @param textDrawContext Styling settings for text drawing and layout
  * @param additionalDrawers Additional custom drawing (default = empty)
  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
  */
class TextLabel(override val parentHierarchy: ComponentHierarchy, val text: LocalizedString,
                override val textDrawContext: TextDrawContext, additionalDrawers: Seq[CustomDrawer] = Vector(),
                override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with TextComponent
{
	// ATTRIBUTES	-----------------------------
	
	override val measuredText = measure(text)
	override val customDrawers = additionalDrawers.toVector :+
		TextDrawer(measuredText, textDrawContext.font, textDrawContext.insets, textDrawContext.color, textDrawContext.alignment)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def updateLayout() = ()
}
