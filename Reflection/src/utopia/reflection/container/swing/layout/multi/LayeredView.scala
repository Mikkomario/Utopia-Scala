package utopia.reflection.container.swing.layout.multi

import utopia.firmament.drawing.mutable.{MutableCustomDrawable, MutableCustomDrawableWrapper}
import utopia.paradigm.enumeration.Alignment
import utopia.genesis.graphics.DrawLevel2
import utopia.reflection.component.swing.template.{AwtComponentWrapperWrapper, SwingComponentRelated}
import utopia.reflection.component.template.layout.Alignable
import utopia.reflection.component.template.layout.stack.CachingReflectionStackable
import utopia.reflection.container.stack.template.StackContainerLike
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.layout.wrapper.AlignFrame
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}

/**
 * This container consists of two views, one in the background and another in the foreground
 * @author Mikko Hilpinen
 * @since 18.1.2020, v1
 */
class LayeredView[Background <: AwtStackable with MutableCustomDrawable, Foreground <: AwtStackable with MutableCustomDrawable]
(background: Background, foreground: Foreground, initialAlignment: Alignment)
	extends AwtComponentWrapperWrapper with CachingReflectionStackable with SwingComponentRelated with AwtContainerRelated
		with MutableCustomDrawableWrapper with Alignable with StackContainerLike[AwtStackable]
{
	// ATTRIBUTES	---------------------
	
	private val foregroundContainer = new AlignFrame(foreground, initialAlignment)
	private val panel = new Panel[AwtStackable]()
	
	
	// INITIAL CODE	---------------------
	
	panel += foregroundContainer
	panel += background
	
	// Updates content layout each time this component is resized
	addResizeListener(updateLayout())
	
	// Foreground needs to be redrawn whenever the background is redrawn
	background.addCustomDrawer(DrawLevel2.Foreground) { (_, _) => foreground.repaint() }
	
	
	// IMPLEMENTED	---------------------
	
	override def components = panel.components
	
	override def component = panel.component
	
	override protected def wrapped = panel
	
	override protected def updateVisibility(visible: Boolean) = super.visible_=(visible)
	
	override def drawable = panel
	
	override def align(alignment: Alignment) = foregroundContainer.align(alignment)
	
	// The stack size is defined by both the background and foreground
	override def calculatedStackSize = background.stackSize max foregroundContainer.stackSize
	
	override def updateLayout() =
	{
		// Both background and foreground are set to span this component's area
		background.size = size
		foregroundContainer.size = size
	}
}
