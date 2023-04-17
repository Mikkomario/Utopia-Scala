package utopia.reach.component.label.text

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.component.text.{MutableStyleTextComponent, TextComponent}
import utopia.firmament.context.TextContext
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
import utopia.firmament.drawing.immutable.BackgroundDrawer
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.stack.StackInsets

object MutableViewTextLabel extends FromGenericContextComponentFactoryFactory[TextContext,
	MutableViewTextLabelFactory, ContextualMutableViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableViewTextLabelFactory(hierarchy)
}

class MutableViewTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromGenericContextFactory[TextContext, ContextualMutableViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: TextContext](context: N) =
		ContextualMutableViewTextLabelFactory(this, context)
	
	
	// OTHER	--------------------------------
	
	/**
	  * Creates a new label that uses the specified content pointer
	  * @param pointer Pointer used by this label
	  * @param font Font used when drawing text
	  * @param displayFunction Function used for converting content to text (default = toString)
	  * @param textColor Color used when drawing text (default = standard black)
	  * @param alignment Alignment used when placing text (default = Left)
	  * @param insets Insets placed around the text (default = any, preferring 0)
	  * @param betweenLinesMargin Margin placed between lines of text, in case there are many (default = 0.0)
	  * @param allowLineBreaks Whether line breaks within text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def forPointer[A](pointer: PointerWithEvents[A], font: Font,
					  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					  textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
					  insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
					  allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		new MutableViewTextLabel[A](parentHierarchy, pointer, TextDrawContext(font, textColor, alignment, insets,
			betweenLinesMargin, allowLineBreaks), displayFunction, allowTextShrink)
	
	/**
	  * Creates a new mutable text view label
	  * @param initialValue Initially displayed value
	  * @param font Font used when drawing text
	  * @param displayFunction Function used for converting content to text (default = toString)
	  * @param textColor Color used when drawing text (default = standard black)
	  * @param alignment Alignment used when placing text (default = Left)
	  * @param insets Insets placed around the text (default = any, preferring 0)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def apply[A](initialValue: A, font: Font, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
				 insets: StackInsets = StackInsets.any, betweenLinesMargin: Double = 0.0,
				 allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		forPointer[A](new PointerWithEvents[A](initialValue), font, displayFunction, textColor, alignment, insets,
			betweenLinesMargin, allowLineBreaks, allowTextShrink)
}

case class ContextualMutableViewTextLabelFactory[+N <: TextContext](labelFactory: MutableViewTextLabelFactory,
																		context: N)
	extends GenericContextualFactory[N, TextContext, ContextualMutableViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[N2 <: TextContext](newContext: N2) = copy(context = newContext)
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Creates a new label that reflects the specified mutable pointer. Uses contextual information.
	  * @param pointer A pointer this label uses
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHint Whether this label should be considered a hint (affects text color) (default = false)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def forPointer[A](pointer: PointerWithEvents[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw,
					  isHint: Boolean = false) =
		labelFactory.forPointer(pointer, context.font, displayFunction,
			if (isHint) context.hintTextColor else context.textColor, context.textAlignment, context.textInsets,
			context.betweenLinesMargin.optimal, context.allowLineBreaks, context.allowTextShrink)
	
	/**
	  * Creates a new mutable view label. Uses contextual information.
	  * @param initialValue Initially displayed value on this label
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHint Whether this label should be considered a hint (affects text color) (default = false)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def apply[A](initialValue: A, displayFunction: DisplayFunction[A] = DisplayFunction.raw, isHint: Boolean = false) =
		forPointer(new PointerWithEvents[A](initialValue), displayFunction, isHint)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param background      Label background color
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHint          Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def forPointerWithCustomBackground[A](contentPointer: PointerWithEvents[A], background: Color,
	                                      displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                                      isHint: Boolean = false) =
	{
		
		val label = mapContext { _.against(background) }.forPointer(contentPointer, displayFunction, isHint)
		label.addCustomDrawer(BackgroundDrawer(background))
		label
	}
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param role            Label background color role
	  * @param preferredShade  Preferred color shade (default = standard)
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHint          Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def forPointerWithBackground[A](contentPointer: PointerWithEvents[A], role: ColorRole,
	                                displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                                preferredShade: ColorLevel = Standard, isHint: Boolean = false) =
		forPointerWithCustomBackground(contentPointer, context.color.preferring(preferredShade)(role),
			displayFunction, isHint)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param initialContent  Content initially displayed on this label
	  * @param background      Label background color
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHint          Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def withCustomBackground[A](initialContent: A, background: Color,
	                            displayFunction: DisplayFunction[A] = DisplayFunction.raw, isHint: Boolean = false) =
		forPointerWithCustomBackground(new PointerWithEvents[A](initialContent), background, displayFunction, isHint)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param initialContent  Initially displayed content on this label
	  * @param role            Label background color role
	  * @param preferredShade  Preferred color shade (default = standard)
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @param isHint          Whether this label should be considered a hint (affects text color)
	  * @return A new label
	  */
	def withBackground[A](initialContent: A, role: ColorRole,
	                      displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                      preferredShade: ColorLevel = Standard, isHint: Boolean = false) =
		forPointerWithBackground(new PointerWithEvents(initialContent), role, displayFunction, preferredShade,
			isHint)
}

/**
  * A mutable implementation of a label that displays items as text
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
  */
class MutableViewTextLabel[A](override val parentHierarchy: ComponentHierarchy,
							  override val contentPointer: PointerWithEvents[A],
							  initialDrawContext: TextDrawContext,
							  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							  override val allowTextShrink: Boolean = false)
	extends MutableCustomDrawReachComponent with MutableStyleTextComponent with TextComponent
		with RefreshableWithPointer[A]
{
	// ATTRIBUTES	----------------------------------
	
	/**
	  * A mutable pointer that contains the currently used text styling
	  */
	val stylePointer = new PointerWithEvents(initialDrawContext)
	/**
	  * Pointer that contains the text currently displayed on this label
	  */
	val textPointer = contentPointer.mergeWith(stylePointer) { (content, style) =>
		measure(displayFunction(content), style)
	}
	
	
	// INITIAL CODE	----------------------------------
	
	// Revalidates and repaints this component on all text changes
	textPointer.addListener { event =>
		if (event.equalsBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	addCustomDrawer(TextViewDrawer(textPointer, stylePointer))
	
	
	// IMPLEMENTED	----------------------------------
	
	override def measuredText = textPointer.value
	
	override def textDrawContext = stylePointer.value
	override def textDrawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def updateLayout() = ()
}
