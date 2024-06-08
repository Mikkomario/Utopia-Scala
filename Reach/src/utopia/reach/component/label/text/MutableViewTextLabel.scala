package utopia.reach.component.label.text

import utopia.firmament.component.display.RefreshableWithPointer
import utopia.firmament.component.text.{MutableStyleTextComponent, TextComponent}
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.localization.DisplayFunction
import utopia.firmament.model.TextDrawContext
import utopia.firmament.model.stack.StackInsets
import utopia.flow.collection.immutable.Empty
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.genesis.text.Font
import utopia.paradigm.color.ColorLevel.Standard
import utopia.paradigm.color.{Color, ColorLevel, ColorRole}
import utopia.paradigm.enumeration.Alignment
import utopia.reach.component.factory.ComponentFactoryFactory.Cff
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.factory.FromContextFactory
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.MutableCustomDrawReachComponent

object MutableViewTextLabel extends Cff[MutableViewTextLabelFactory]
{
	override def apply(hierarchy: ComponentHierarchy) = new MutableViewTextLabelFactory(hierarchy)
}

class MutableViewTextLabelFactory(parentHierarchy: ComponentHierarchy)
	extends FromContextFactory[TextContext, ContextualMutableViewTextLabelFactory]
{
	// IMPLEMENTED	----------------------------
	
	override def withContext(context: TextContext) =
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
	  * @param lineSplitThreshold Width threshold after which lines are split (optional)
	  * @param betweenLinesMargin Margin placed between lines of text, in case there are many (default = 0.0)
	  * @param allowLineBreaks Whether line breaks within text should be respected and applied (default = true)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def forPointer[A](pointer: EventfulPointer[A], font: Font,
	                  displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                  textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
	                  insets: StackInsets = StackInsets.any, lineSplitThreshold: Option[Double] = None,
	                  betweenLinesMargin: Double = 0.0, allowLineBreaks: Boolean = true,
	                  allowTextShrink: Boolean = false) =
		new MutableViewTextLabel[A](parentHierarchy, pointer, TextDrawContext(font, textColor, alignment, insets,
			lineSplitThreshold, betweenLinesMargin, allowLineBreaks), displayFunction, allowTextShrink)
	
	/**
	  * Creates a new mutable text view label
	  * @param initialValue Initially displayed value
	  * @param font Font used when drawing text
	  * @param displayFunction Function used for converting content to text (default = toString)
	  * @param textColor Color used when drawing text (default = standard black)
	  * @param alignment Alignment used when placing text (default = Left)
	  * @param insets Insets placed around the text (default = any, preferring 0)
	  * @param lineSplitThreshold Width threshold after which lines are split (optional)
	  * @param betweenLinesMargin Margin placed between lines of text, in case there are many (default = 0.0)
	  * @param allowTextShrink Whether text should be allowed to shrink to conserve space (default = false)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def apply[A](initialValue: A, font: Font, displayFunction: DisplayFunction[A] = DisplayFunction.raw,
				 textColor: Color = Color.textBlack, alignment: Alignment = Alignment.Left,
				 insets: StackInsets = StackInsets.any, lineSplitThreshold: Option[Double] = None,
				 betweenLinesMargin: Double = 0.0, allowLineBreaks: Boolean = true, allowTextShrink: Boolean = false) =
		forPointer[A](EventfulPointer[A](initialValue), font, displayFunction, textColor, alignment, insets,
			lineSplitThreshold, betweenLinesMargin, allowLineBreaks, allowTextShrink)
}

case class ContextualMutableViewTextLabelFactory(labelFactory: MutableViewTextLabelFactory, context: TextContext,
                                                 customDrawers: Seq[CustomDrawer] = Empty, isHint: Boolean = false)
	extends TextContextualFactory[ContextualMutableViewTextLabelFactory]
		with ContextualBackgroundAssignableFactory[TextContext, ContextualMutableViewTextLabelFactory]
		with CustomDrawableFactory[ContextualMutableViewTextLabelFactory]
{
	// COMPUTED ---------------------------
	
	/**
	  * @return A copy of this factory that creates hint labels
	  */
	def hint = copy(isHint = true)
	
	
	// IMPLEMENTED	----------------------------------
	
	override def self: ContextualMutableViewTextLabelFactory = this
	
	override def withCustomDrawers(drawers: Seq[CustomDrawer]): ContextualMutableViewTextLabelFactory =
		copy(customDrawers = drawers)
	
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Creates a new label that reflects the specified mutable pointer. Uses contextual information.
	  * @param pointer A pointer this label uses
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def forPointer[A](pointer: EventfulPointer[A], displayFunction: DisplayFunction[A] = DisplayFunction.raw) = {
		val label = labelFactory.forPointer(pointer, context.font, displayFunction,
			if (isHint) context.hintTextColor else context.textColor, context.textAlignment, context.textInsets,
			context.lineSplitThreshold,
			context.betweenLinesMargin.optimal, context.allowLineBreaks, context.allowTextShrink)
		customDrawers.foreach(label.addCustomDrawer)
		label
	}
	/**
	  * Creates a new mutable view label. Uses contextual information.
	  * @param initialValue Initially displayed value on this label
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @tparam A Type of displayed content
	  * @return A new label
	  */
	def apply[A](initialValue: A, displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		forPointer(EventfulPointer[A](initialValue), displayFunction)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param background      Label background color
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(Color).forPointer(...) instead", "v1.1")
	def forPointerWithCustomBackground[A](contentPointer: EventfulPointer[A], background: Color,
	                                      displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		withBackground(background).forPointer(contentPointer, displayFunction)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param contentPointer  Pointer that is reflected on this label
	  * @param role            Label background color role
	  * @param preferredShade  Preferred color shade (default = standard)
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(ColorRole).forPointer(...) instead", "v1.1")
	def forPointerWithBackground[A](contentPointer: EventfulPointer[A], role: ColorRole,
	                                displayFunction: DisplayFunction[A] = DisplayFunction.raw,
	                                preferredShade: ColorLevel = Standard) =
		withBackground(role, preferredShade).forPointer(contentPointer, displayFunction)
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param initialContent  Content initially displayed on this label
	  * @param background      Label background color
	  * @param displayFunction Function used when converting content to text (default = toString)
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(Color).apply(...) instead", "v1.1")
	def withCustomBackground[A](initialContent: A, background: Color,
	                            displayFunction: DisplayFunction[A] = DisplayFunction.raw) =
		withBackground(background).apply(initialContent, displayFunction)
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param initialContent  Initially displayed content on this label
	  * @param role            Label background color role
	  * @param displayFunction Function used when converting content to text
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(ColorRole).apply(...) instead", "v1.1")
	def withBackground[A](initialContent: A, role: ColorRole,
	                      displayFunction: DisplayFunction[A]): MutableViewTextLabel[A] =
		withBackground(role).apply(initialContent, displayFunction)
}

/**
  * A mutable implementation of a label that displays items as text
  * @author Mikko Hilpinen
  * @since 21.10.2020, v0.1
  */
class MutableViewTextLabel[A](override val parentHierarchy: ComponentHierarchy,
                              override val contentPointer: EventfulPointer[A],
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
	val stylePointer = EventfulPointer(initialDrawContext)
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
			revalidate()
	}
	addCustomDrawer(TextViewDrawer(textPointer, stylePointer))
	
	
	// IMPLEMENTED	----------------------------------
	
	override def measuredText = textPointer.value
	
	override def textDrawContext = stylePointer.value
	override def textDrawContext_=(newContext: TextDrawContext) = stylePointer.value = newContext
	
	override def updateLayout() = ()
}
