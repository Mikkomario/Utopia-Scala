package utopia.reflection.component.reach.label

import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext}
import utopia.reflection.component.drawing.template.CustomDrawer
import utopia.reflection.component.drawing.view.TextDrawer
import utopia.reflection.component.reach.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.CustomDrawReachComponent
import utopia.reflection.component.template.display.PoolWithPointer
import utopia.reflection.component.template.text.SingleLineTextComponent2
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

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
	  * @param font Font used when drawing the text
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param textColor Color used when drawing the text (default = standard black)
	  * @param alignment Text alignment (default = left)
	  * @param insets Insets around the text (default = any insets, preferring zero)
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param allowTextShrink Whether text should be allowed to shrink below its standard size if necessary (default = false)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], font: Font, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
				 insets: StackInsets = StackInsets.any, additionalDrawers: Seq[CustomDrawer] = Vector(),
				 allowTextShrink: Boolean = false) =
		new ViewTextLabel(parentHierarchy, contentPointer, TextDrawContext(font, textColor, alignment, insets),
			displayFunction, additionalDrawers, allowTextShrink)
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
		  * @param additionalDrawers Additional custom drawing (default = empty)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withCustomBackground[A](contentPointer: Changing[A], background: ComponentColor,
									displayFunction: DisplayFunction[A] = DisplayFunction.raw,
									additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		{
			f.mapContext { _.inContextWithBackground(background) }(contentPointer, displayFunction,
				new BackgroundDrawer(background) +: additionalDrawers, isHint)
		}
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param contentPointer Pointer that is reflected on this label
		  * @param role Label background color role
		  * @param preferredShade Preferred color shade (default = standard)
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param additionalDrawers Additional custom drawing (default = empty)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withBackground[A](contentPointer: Changing[A], role: ColorRole,
							  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							  preferredShade: ColorShade = Standard,
							  additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
			withCustomBackground(contentPointer, f.context.color(role, preferredShade), displayFunction,
				additionalDrawers, isHint)
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
	  * @param additionalDrawers Additional custom drawing (default = empty)
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def apply[A](contentPointer: Changing[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 additionalDrawers: Seq[CustomDrawer] = Vector(), isHint: Boolean = false) =
		factory(contentPointer, context.font, displayFunction, if (isHint) context.hintTextColor else context.textColor,
			context.textAlignment, context.textInsets, additionalDrawers, !context.textHasMinWidth)
}

/**
  * A text label that displays contents of a changing item
  * @author Mikko Hilpinen
  * @since 17.10.2020, v2
  */
class ViewTextLabel[A](override val parentHierarchy: ComponentHierarchy, override val contentPointer: Changing[A],
					override val drawContext: TextDrawContext, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					additionalDrawers: Seq[CustomDrawer] = Vector(),
					override val allowTextShrink: Boolean = false)
	extends CustomDrawReachComponent with SingleLineTextComponent2 with PoolWithPointer[A, Changing[A]]
{
	// ATTRIBUTE	-------------------------------------
	
	val textPointer = contentPointer.map { displayFunction(_) }
	val customDrawers = TextDrawer(textPointer, Changing.wrap(drawContext)) +: additionalDrawers.toVector
	
	
	// INITIAL CODE	-------------------------------------
	
	// Revalidates and repaints this component on all text changes
	textPointer.addListener { _ => revalidateAndThen { repaint() } }
	
	
	// IMPLEMENTED	-------------------------------------
	
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
	
	override def text = textPointer.value
	
	override def updateLayout() = ()
}
