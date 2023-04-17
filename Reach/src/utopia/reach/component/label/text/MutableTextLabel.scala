package utopia.reach.component.label.text

import utopia.firmament.component.text.{MutableTextComponent, TextComponent}
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.model.TextDrawContext
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.{FromGenericContextFactory, FromGenericContextComponentFactoryFactory, GenericContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.MutableCustomDrawReachComponent
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.stack.StackInsets

object MutableTextLabel extends FromGenericContextComponentFactoryFactory[TextContext, MutableTextLabelFactory,
	ContextualMutableTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = MutableTextLabelFactory(hierarchy)
}

case class MutableTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[TextContext, ContextualMutableTextLabelFactory]
{
	override def withContext[N <: TextContext](context: N) =
		ContextualMutableTextLabelFactory(parentHierarchy, context)
}

case class ContextualMutableTextLabelFactory[+N <: TextContext](parentHierarchy: ComponentHierarchy,
																	override val context: N)
	extends GenericContextualFactory[N, TextContext, ContextualMutableTextLabelFactory]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[C2 <: TextContext](newContext: C2) =
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
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text       Text displayed on this label
	  * @param background Label background color
	  * @param isHint     Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def withCustomBackground(text: LocalizedString, background: Color, isHint: Boolean = false) =
	{
		val label = mapContext { _.against(background) }(text, isHint)
		label.addCustomDrawer(BackgroundDrawer(background))
		label
	}
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text           Text displayed on this label
	  * @param role           Label background color role
	  * @param preferredShade Preferred color shade (default = standard)
	  * @param isHint         Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def withBackground(text: LocalizedString, role: ColorRole,
	                   preferredShade: ColorLevel = Standard, isHint: Boolean = false) =
		withCustomBackground(text, context.color.preferring(preferredShade)(role), isHint)
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
	extends MutableCustomDrawReachComponent with TextComponent with MutableTextComponent
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
		if (event.equalsBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	addCustomDrawer(TextViewDrawer(measuredTextPointer, stylePointer))
	
	
	// IMPLEMENTED	-------------------------
	
	override def measuredText = measuredTextPointer.value
	
	override def toString = s"Label($text)"
	
	override def text = textPointer.value
	override def text_=(newText: LocalizedString) = textPointer.value = newText
	
	override def textDrawContext = stylePointer.value
	override def textDrawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def updateLayout() = ()
}
