package utopia.reflection.component.reach.label

import utopia.genesis.color.Color
import utopia.genesis.shape.shape2D.Bounds
import utopia.genesis.util.Drawer
import utopia.reflection.component.drawing.immutable.TextDrawContext
import utopia.reflection.component.drawing.mutable.TextDrawer
import utopia.reflection.component.reach.hierarchy.ComponentHierarchy
import utopia.reflection.component.reach.template.MutableCustomDrawReachComponent
import utopia.reflection.component.template.text.{MutableTextComponent, SingleLineTextComponent2}
import utopia.reflection.localization.LocalizedString
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.stack.StackInsets
import utopia.reflection.text.Font

/**
  * A fully mutable label that displays text
  * @author Mikko Hilpinen
  * @since 4.10.2020, v2
  */
class MutableTextLabel(override val parentHierarchy: ComponentHierarchy, initialText: LocalizedString,
					   initialFont: Font, initialTextColor: Color = Color.textBlack,
					   initialInsets: StackInsets = StackInsets.any, initialAlignment: Alignment = Alignment.Left,
					   override val allowTextShrink: Boolean = false) extends MutableCustomDrawReachComponent
	with SingleLineTextComponent2 with MutableTextComponent
{
	// ATTRIBUTES	-------------------------
	
	private val drawer = TextDrawer(initialText, TextDrawContext(initialFont, initialTextColor, initialAlignment,
		initialInsets))
	
	
	// INITIAL CODE	-------------------------
	
	// Revalidates and/or repaints this component whenever content or styling changes
	drawer.textPointer.addListener { _ =>
		revalidate()
		// TODO: Repaint should only happen after the revalidation process has completed (implement)
		repaintParent()
	}
	drawer.contextPointer.addListener { event =>
		if (event.newValue.hasSameDimensionsAs(event.oldValue))
			repaint()
		else
		{
			revalidate()
			// TODO: Here also, repaint only after revalidation
			repaintParent()
		}
	}
	
	
	// IMPLEMENTED	-------------------------
	
	override def text = drawer.text
	override def text_=(newText: LocalizedString) = drawer.text = newText
	
	override def drawContext = drawer.drawContext
	override def drawContext_=(newContext: TextDrawContext) = drawer.drawContext = newContext
	
	override protected def drawContent(drawer: Drawer, clipZone: Option[Bounds]) = ()
	
	override def updateLayout() = ()
}
