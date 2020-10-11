package utopia.reflection.component.reach.label

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, ColorContextLike, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext, TextDrawer}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{ComponentFactoryFactory, CustomDrawReachComponent}
import utopia.reflection.component.template.text.SingleLineTextComponent2
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object StaticTextLabel extends ComponentFactoryFactory[StaticTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = StaticTextLabelFactory(hierarchy)
}

/**
  * Used for constructing new static text labels
  * @param parentHierarchy A component hierarchy the new labels will be placed in
  */
case class StaticTextLabelFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	  * Creates a new text label
	  * @param text Text displayed on this label
	  * @param font Font used when drawing the text
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param alignment Text alignment (default = left)
	  * @param insets Insets around the text (default = any insets, preferring zero)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply(text: LocalizedString, font: Font, textColor: Color = Color.textBlack,
			  alignment: Alignment = Alignment.Left, insets: StackInsets = StackInsets.any,
			  additionalDrawers: Seq[CustomDrawer] = Vector(), allowTextShrink: Boolean = false) =
		new StaticTextLabel(parentHierarchy, text, TextDrawContext(font, textColor, alignment, insets),
			additionalDrawers, allowTextShrink)
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextual(text: LocalizedString, additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false)
				  (implicit context: TextContextLike) =
		apply(text, context.font, if (isHint) context.hintTextColor else context.textColor,
			context.textAlignment, context.textInsets, additionalDrawers, !context.textHasMinWidth)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text Text displayed on this label
	  * @param background Label background color
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithCustomBackground(text: LocalizedString, background: ComponentColor,
									   additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false)
									  (implicit context: BackgroundSensitive[TextContextLike]) =
	{
		implicit val c: TextContextLike = context.inContextWithBackground(background)
		contextual(text, new BackgroundDrawer(c.containerBackground) +: additionalDrawers, isHint)
	}
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text Text displayed on this label
	  * @param role Label background color role
	  * @param preferredShade Preferred color shade (default = standard)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def contextualWithBackground(text: LocalizedString, role: ColorRole, preferredShade: ColorShade = Standard,
								 additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false)
								(implicit context: ColorContextLike with BackgroundSensitive[TextContextLike]) =
		contextualWithCustomBackground(text, context.color(role, preferredShade), additionalDrawers, isHint)
}

/**
  * This text label doesn't allow content or styling modifications from outside but presents static text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  * @param parentHierarchy This component's parent hierarchy
  * @param text Text displayed on this label
  * @param drawContext Styling settings for text drawing and layout
  * @param additionalDrawers Additional custom drawing (default = empty)
  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
  */
class StaticTextLabel(override val parentHierarchy: ComponentHierarchy, override val text: LocalizedString,
					  override val drawContext: TextDrawContext,
					  additionalDrawers: Seq[CustomDrawer] = Vector(),
					  override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with SingleLineTextComponent2
{
	// ATTRIBUTES	-----------------------------
	
	override val customDrawers = additionalDrawers.toVector :+ new TextDrawer(text, drawContext)
	
	
	// IMPLEMENTED	-----------------------------
	
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
	
	override def updateLayout() = ()
}
