package utopia.reflection.component.reach.label

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.component.context.{BackgroundSensitive, ColorContextLike, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext}
import utopia.reflection.component.drawing.mutable.TextDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.{ComponentFactoryFactory, MutableCustomDrawReachComponent}
import utopia.reflection.component.template.text.{MutableTextComponent, SingleLineTextComponent2}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object MutableTextLabel extends ComponentFactoryFactory[MutableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = MutableTextLabelFactory(hierarchy)
}

case class MutableTextLabelFactory(parentHierarchy: ComponentHierarchy)
{
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def apply(text: LocalizedString, isHint: Boolean = false)(implicit context: TextContextLike) =
		new MutableTextLabel(parentHierarchy, text, context.font,
			if (isHint) context.hintTextColor else context.textColor, context.textAlignment,
			context.textInsets, !context.textHasMinWidth)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text Text displayed on this label
	  * @param background Label background color
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def withCustomBackground(text: LocalizedString, background: ComponentColor, isHint: Boolean = false)
									  (implicit context: BackgroundSensitive[TextContextLike]) =
	{
		implicit val c: TextContextLike = context.inContextWithBackground(background)
		val label = apply(text, isHint)
		label.addCustomDrawer(new BackgroundDrawer(background))
		label
	}
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text Text displayed on this label
	  * @param role Label background color role
	  * @param preferredShade Preferred color shade (default = standard)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @param context Implicit component creation context
	  * @return A new label
	  */
	def withBackground(text: LocalizedString, role: ColorRole,
								 preferredShade: ColorShade = Standard, isHint: Boolean = false)
								(implicit context: ColorContextLike with BackgroundSensitive[TextContextLike]) =
		withCustomBackground(text, context.color(role, preferredShade), isHint)
}

/**
  * A fully mutable label that displays text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
class MutableTextLabel(override val parentHierarchy: ComponentHierarchy, initialText: LocalizedString,
					   initialFont: Font, initialTextColor: Color = Color.textBlack,
					   initialAlignment: Alignment = Alignment.Left, initialInsets: StackInsets = StackInsets.any,
					   override val allowTextShrink: Boolean = false) extends MutableCustomDrawReachComponent
	with SingleLineTextComponent2 with MutableTextComponent
{
	// ATTRIBUTES	-------------------------
	
	private val drawer = TextDrawer(initialText, TextDrawContext(initialFont, initialTextColor, initialAlignment,
		initialInsets))
	
	
	// INITIAL CODE	-------------------------
	
	// Revalidates and/or repaints this component whenever content or styling changes
	drawer.textPointer.addListener { _ => revalidateAndThen { repaint() } }
	drawer.contextPointer.addListener { event =>
		if (event.newValue.hasSameDimensionsAs(event.oldValue))
			repaint()
		else
			revalidateAndThen { repaint() }
	}
	addCustomDrawer(drawer)
	
	
	// IMPLEMENTED	-------------------------
	
	override def toString = s"Label($text)"
	
	override def text = drawer.text
	override def text_=(newText: LocalizedString) = drawer.text = newText
	
	override def drawContext = drawer.drawContext
	override def drawContext_=(newContext: TextDrawContext) = drawer.drawContext = newContext
	
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
	
	override def updateLayout() = ()
}
