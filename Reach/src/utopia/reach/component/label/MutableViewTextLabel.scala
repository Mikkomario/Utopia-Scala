package utopia.reach.component.label

import utopia.flow.datastructure.mutable.PointerWithEvents
import utopia.genesis.color.Color
import utopia.reflection.color.{ColorRole, ColorShade, ComponentColor}
import utopia.reflection.color.ColorShade.Standard
import utopia.reflection.component.context.{BackgroundSensitive, TextContextLike}
import utopia.reflection.component.drawing.immutable.{BackgroundDrawer, TextDrawContext}
import utopia.reflection.component.drawing.view.TextViewDrawer2
import utopia.reach.component.factory.{ContextInsertableComponentFactory, ContextInsertableComponentFactoryFactory, ContextualComponentFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.MutableCustomDrawReachComponent
import utopia.reflection.component.template.display.RefreshableWithPointer
import utopia.reflection.component.template.text.{MutableStyleTextComponent, TextComponent2}
import utopia.reflection.localization.DisplayFunction
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.{Font, FontMetricsContext, MeasuredText}

object MutableViewTextLabel extends ContextInsertableComponentFactoryFactory[TextContextLike,
	MutableViewTextLabelFactory, ContextualMutableViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableViewTextLabelFactory(hierarchy)
}

class MutableViewTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends ContextInsertableComponentFactory[TextContextLike, ContextualMutableViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext[N <: TextContextLike](context: N) =
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
			betweenLinesMargin), displayFunction, allowLineBreaks, allowTextShrink)
	
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

object ContextualMutableViewTextLabelFactory
{
	// EXTENSIONS	-----------------------------
	
	implicit class ColorChangingMutableViewTextLabelFactory[N <: TextContextLike with BackgroundSensitive[TextContextLike]]
	(val f: ContextualMutableViewTextLabelFactory[N]) extends AnyVal
	{
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param contentPointer Pointer that is reflected on this label
		  * @param background Label background color
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def forPointerWithCustomBackground[A](contentPointer: PointerWithEvents[A], background: ComponentColor,
											  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
											  isHint: Boolean = false) =
		{
			
			val label = f.mapContext { _.inContextWithBackground(background) }.forPointer(contentPointer,
				displayFunction, isHint)
			label.addCustomDrawer(BackgroundDrawer(background))
			label
		}
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param contentPointer Pointer that is reflected on this label
		  * @param role Label background color role
		  * @param preferredShade Preferred color shade (default = standard)
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def forPointerWithBackground[A](contentPointer: PointerWithEvents[A], role: ColorRole,
										displayFunction: DisplayFunction[A] = DisplayFunction.raw,
										preferredShade: ColorShade = Standard, isHint: Boolean = false) =
			forPointerWithCustomBackground(contentPointer, f.context.color(role, preferredShade), displayFunction,
				isHint)
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param initialContent Content initially displayed on this label
		  * @param background Label background color
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withCustomBackground[A](initialContent: A, background: ComponentColor,
									displayFunction: DisplayFunction[A] = DisplayFunction.raw, isHint: Boolean = false) =
			forPointerWithCustomBackground(new PointerWithEvents[A](initialContent), background, displayFunction, isHint)
		
		/**
		  * Creates a new text label with solid background utilizing contextual information
		  * @param initialContent Initially displayed content on this label
		  * @param role Label background color role
		  * @param preferredShade Preferred color shade (default = standard)
		  * @param displayFunction Function used when converting content to text (default = toString)
		  * @param isHint Whether this label should be considered a hint (affects text color)
		  * @return A new label
		  */
		def withBackground[A](initialContent: A, role: ColorRole,
							  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							  preferredShade: ColorShade = Standard, isHint: Boolean = false) =
			forPointerWithBackground(new PointerWithEvents(initialContent), role, displayFunction, preferredShade,
				isHint)
	}
}

case class ContextualMutableViewTextLabelFactory[+N <: TextContextLike](labelFactory: MutableViewTextLabelFactory,
																		context: N)
	extends ContextualComponentFactory[N, TextContextLike, ContextualMutableViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------------
	
	override def withContext[N2 <: TextContextLike](newContext: N2) = copy(context = newContext)
	
	
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
}

/**
  * A mutable implementation of a label that displays items as text
  * @author Mikko Hilpinen
  * @since 21.10.2020, v2
  */
class MutableViewTextLabel[A](override val parentHierarchy: ComponentHierarchy,
							  override val contentPointer: PointerWithEvents[A],
							  initialDrawContext: TextDrawContext,
							  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
							  allowLineBreaks: Boolean = true, override val allowTextShrink: Boolean = false)
	extends MutableCustomDrawReachComponent with MutableStyleTextComponent with TextComponent2
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
	val textPointer = contentPointer.mergeWith(stylePointer) { (content, style) => MeasuredText(displayFunction(content),
		FontMetricsContext(fontMetrics(style.font), style.betweenLinesMargin), style.alignment, allowLineBreaks) }
	
	
	// INITIAL CODE	----------------------------------
	
	// Revalidates and repaints this component on all text changes
	textPointer.addListener { event =>
		if (event.compareBy { _.size })
			repaint()
		else
			revalidateAndRepaint()
	}
	addCustomDrawer(TextViewDrawer2(textPointer, stylePointer))
	
	
	// IMPLEMENTED	----------------------------------
	
	override def measuredText = textPointer.value
	
	override def drawContext = stylePointer.value
	override def drawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def updateLayout() = ()
}
