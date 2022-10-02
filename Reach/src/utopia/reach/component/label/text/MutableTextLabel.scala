package utopia.reach.component.label.text

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.paradigm.color.Color
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.MutableCustomDrawReachComponent
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.component.context.{BackgroundSensitive, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext}
import utopia.reflection.component.drawing.view.TextViewDrawer2
import utopia.reflection.component.template.text.{MutableTextComponent, TextComponent2}
import utopia.reflection.localization.LocalizedString
import utopia.paradigm.enumeration.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

object MutableTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike, MutableTextLabelFactory,
	ContextualMutableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = MutableTextLabelFactory(hierarchy)
}

case class MutableTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualMutableTextLabelFactory]
{
	override def withContext[N <: TextContextLike](context: N) =
		ContextualMutableTextLabelFactory(parentHierarchy, context)
}

object ContextualMutableTextLabelFactory
{
	// EXTENSIONS	----------------------------------
	
	implicit class ColorChangingMutableTextLabelFactory[N <: TextContextLike with BackgroundSensitive[TextContextLike]]
	(val f: ContextualMutableTextLabelFactory[N]) extends AnyVal
	{
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param text Text displayed on this label
		  * @param background Label background color
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withCustomBackground(text: LocalizedString, background: ComponentColor, isHint: Boolean = false) =
		{
			val label = f.mapContext { _.inContextWithBackground(background) }(text, isHint)
			label.addCustomDrawer(BackgroundDrawer(background))
			label
		}
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param text Text displayed on this label
		  * @param role Label background color role
		  * @param preferredShade Preferred color shade (default = standard)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withBackground(text: LocalizedString, role: ColorRole,
						   preferredShade: ColorShade = Standard, isHint: Boolean = false) =
			withCustomBackground(text, f.context.color(role, preferredShade), isHint)
	}
}

case class ContextualMutableTextLabelFactory[+N <: TextContextLike](parentHierarchy: ComponentHierarchy,
																	override val context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualMutableTextLabelFactory]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[C2 <: TextContextLike](newContext: C2) =
		copy(context = newContext)
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @param isHint Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def apply(text: LocalizedString, isHint: Boolean = false) =
		new MutableTextLabel(parentHierarchy, text, context.font,
			if (isHint) context.hintTextColor else context.textColor, context.textAlignment, context.textInsets,
			context.betweenLinesMargin.optimal, context.allowLineBreaks, context.allowTextShrink)
}

/**
  * A fully mutable label that displays text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
class MutableTextLabel(override val parentHierarchy: ComponentHierarchy, initialText: LocalizedString,
					   initialFont: Font, initialTextColor: Color = Color.textBlack,
					   initialAlignment: Alignment = Alignment.Left, initialInsets: StackInsets = StackInsets.any,
					   initialBetweenLinesMargin: Double = 0.0, allowLineBreaks: Boolean = false,
					   override val allowTextShrink: Boolean = false)
	extends MutableCustomDrawReachComponent with TextComponent2 with MutableTextComponent
{
	// ATTRIBUTES	-------------------------
	
	/**
	  * A mutable pointer that contains this label's style
	  */
	val stylePointer = new PointerWithEvents(TextDrawContext(initialFont, initialTextColor, initialAlignment,
		initialInsets, initialBetweenLinesMargin, allowLineBreaks))
	/**
	  * A mutable pointer that contains this label's text
	  */
	val textPointer = new PointerWithEvents(initialText)
	/**
	  * A pointer to this label's measured text
	  */
	val measuredTextPointer = textPointer.mergeWith(stylePointer)(measure)
	
	
	// INITIAL CODE	-------------------------
	
	// Revalidates and/or repaints this component whenever content or styling changes
	measuredTextPointer.addContinuousListener { event =>
		if (event.compareBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	addCustomDrawer(TextViewDrawer2(measuredTextPointer, stylePointer))
	
	
	// IMPLEMENTED	-------------------------
	
	override def measuredText = measuredTextPointer.value
	
	override def toString = s"Label($text)"
	
	override def text = textPointer.value
	override def text_=(newText: LocalizedString) = textPointer.value = newText
	
	override def drawContext = stylePointer.value
	override def drawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def updateLayout() = ()
}
