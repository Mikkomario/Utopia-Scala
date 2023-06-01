package utopia.reach.component.label.text

import utopia.firmament.component.text.TextComponent
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.{BackgroundDrawer, CustomDrawableFactory, TextDrawer}
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.{StackInsets, StackInsetsConvertible}
import utopia.genesis.text.Font
import utopia.paradigm.color.{Color, ColorRole}
import utopia.paradigm.enumeration.{Alignment, FromAlignmentFactory}
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.factory.{BackgroundAssignable, FramedFactory, FromContextFactory}
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
case class TextLabelFactory(parentHierarchy: ComponentHierarchy, alignment: Alignment = Alignment.Left,
                            insets: StackInsets = StackInsets.any, customDrawers: Vector[CustomDrawer] = Vector())
	extends FromContextFactory[TextContext, ContextualTextLabelFactory] with FramedFactory[TextLabelFactory]
		with CustomDrawableFactory[TextLabelFactory] with BackgroundAssignable[TextLabelFactory]
		with FromAlignmentFactory[TextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def apply(alignment: Alignment) = copy(alignment = alignment)
	
	override def withInsets(insets: StackInsetsConvertible): TextLabelFactory = copy(insets = insets.toInsets)
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): TextLabelFactory = copy(customDrawers = drawers)
	
	override def withBackground(background: Color): TextLabelFactory = withCustomDrawer(BackgroundDrawer(background))
	
	override def withContext(context: TextContext) = ContextualTextLabelFactory(parentHierarchy, context, customDrawers)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new text label
	  * @param text Text displayed on this label
	  * @param font Font used when drawing the text
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param betweenLinesMargin Margin placed between lines of text when line breaks are used (default = 0)
	  * @param allowLineBreaks Whether line breaks in the text should be recognized and respected (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply(text: LocalizedString, font: Font, textColor: Color = Color.textBlack, betweenLinesMargin: Double = 0.0,
	          allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		new TextLabel(parentHierarchy, text, TextDrawContext(font, textColor, alignment, insets,
			betweenLinesMargin, allowLineBreaks), customDrawers, allowTextShrink)
}

case class ContextualTextLabelFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                      customDrawers: Vector[CustomDrawer] = Vector(), isHint: Boolean = false)
	extends TextContextualFactory[ContextualTextLabelFactory]
		with ContextualBackgroundAssignableFactory[TextContext, ContextualTextLabelFactory]
		with CustomDrawableFactory[ContextualTextLabelFactory]
{
	// COMPUTED ---------------------------------
	
	/**
	  * @return Copy of this factory that creates hint labels
	  */
	def hint = copy(isHint = true)
	
	
	// IMPLEMENTED	-----------------------------
	
	override def self: ContextualTextLabelFactory = this
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualTextLabelFactory =
		copy(customDrawers = drawers)
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	
	// OTHER	---------------------------------
	
	/**
	  * @param isHint Whether hint labels should be constructed
	  * @return Copy of this factory with the specified setting
	  */
	def withIsHint(isHint: Boolean) = copy(isHint = isHint)
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @return A new label
	  */
	def apply(text: LocalizedString) =
		new TextLabel(parentHierarchy, text,
			TextDrawContext(context.font, if (isHint) context.hintTextColor else context.textColor,
				context.textAlignment, context.textInsets, context.betweenLinesMargin.optimal, context.allowLineBreaks),
			customDrawers, context.allowTextShrink)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text              Text displayed on this label
	  * @param background        Label background color
	  * @return A new label
	  */
	@deprecated("Replaced with .withBackground(Color).apply(LocalizedString)", "v1.1")
	def withCustomBackground(text: LocalizedString, background: Color) = withBackground(background)(text)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text              Text displayed on this label
	  * @param role              Label background color role
	  * @return A new label
	  */
	@deprecated("Replaced with .withBackground(ColorRole).apply(LocalizedString)", "v1.1")
	def withBackground(text: LocalizedString, role: ColorRole): TextLabel = withBackground(role).apply(text)
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
