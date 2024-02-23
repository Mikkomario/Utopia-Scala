package utopia.reach.component.label.text

import utopia.firmament.component.text.{MutableTextComponent, TextComponent}
import utopia.firmament.context.TextContext
import utopia.firmament.drawing.immutable.CustomDrawableFactory
import utopia.firmament.drawing.template.CustomDrawer
import utopia.firmament.drawing.view.TextViewDrawer
import utopia.firmament.localization.LocalizedString
import utopia.firmament.model.TextDrawContext
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.paradigm.color.{Color, ColorRole}
import utopia.reach.component.factory.FromContextComponentFactoryFactory.Ccff
import utopia.reach.component.factory.contextual.{ContextualBackgroundAssignableFactory, TextContextualFactory}
import utopia.reach.component.hierarchy.ComponentHierarchy
import utopia.reach.component.template.MutableCustomDrawReachComponent

object MutableTextLabel extends Ccff[TextContext, ContextualMutableTextLabelFactory]
{
	override def withContext(hierarchy: ComponentHierarchy, context: TextContext) =
		ContextualMutableTextLabelFactory(hierarchy, context)
}

case class ContextualMutableTextLabelFactory(parentHierarchy: ComponentHierarchy, context: TextContext,
                                             customDrawers: Vector[CustomDrawer] = Vector.empty, isHint: Boolean = false)
	extends TextContextualFactory[ContextualMutableTextLabelFactory]
		with ContextualBackgroundAssignableFactory[TextContext, ContextualMutableTextLabelFactory]
		with CustomDrawableFactory[ContextualMutableTextLabelFactory]
{
	// COMPUTED --------------------------------------
	
	/**
	  * @return Copy of this factory that creates hint labels
	  */
	def hint = copy(isHint = true)
	
	
	// IMPLEMENTED	----------------------------------
	
	override def self: ContextualMutableTextLabelFactory = this
	
	override def withCustomDrawers(drawers: Vector[CustomDrawer]): ContextualMutableTextLabelFactory =
		copy(customDrawers = drawers)
	override def withContext(newContext: TextContext) = copy(context = newContext)
	
	
	// OTHER	--------------------------------------
	
	/**
	  * Creates a new text label utilizing contextual information
	  * @param text Text displayed on this label
	  * @return A new label
	  */
	def apply(text: LocalizedString) = {
		val label = new MutableTextLabel(parentHierarchy, text, TextDrawContext.createContextual(isHint)(context),
			context.allowTextShrink)
		customDrawers.foreach(label.addCustomDrawer)
		label
	}
	
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text       Text displayed on this label
	  * @param background Label background color
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(Color).apply(LocalizedString) instead", "v1.1")
	def withCustomBackground(text: LocalizedString, background: Color) =
		withBackground(background)(text)
	/**
	  * Creates a new text label with solid background utilizing contextual information
	  * @param text Text displayed on this label
	  * @param role Label background color role
	  * @return A new label
	  */
	@deprecated("Please use .withBackground(ColorRole).apply(LocalizedString) instead", "v1.1")
	def withBackground(text: LocalizedString, role: ColorRole): MutableTextLabel =
		withBackground(role).apply(text)
}

/**
  * A fully mutable label that displays text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v0.1
  */
class MutableTextLabel(override val parentHierarchy: ComponentHierarchy, initialText: LocalizedString,
                       initialStyle: TextDrawContext, override val allowTextShrink: Boolean = false)
	extends MutableCustomDrawReachComponent with TextComponent with MutableTextComponent
{
	// ATTRIBUTES	-------------------------
	
	/**
	  * A mutable pointer that contains this label's style
	  */
	val stylePointer = EventfulPointer(initialStyle)
	/**
	  * A mutable pointer that contains this label's text
	  */
	val textPointer = EventfulPointer(initialText)
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
			revalidate()
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
