package utopia.reflection.container.swing.layout.wrapper

import utopia.genesis.shape.shape2D.Direction2D
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.component.template.layout.Alignable
import utopia.reflection.container.stack.template.layout.AlignFrameLike
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.shape.Alignment
import utopia.reflection.shape.Alignment.Center

object AlignFrame
{
	/**
	  * Creates a frame that places content at center
	  * @param content Content for the frame
	  * @tparam C Type of frame content
	  * @return A new frame
	  */
	def centered[C <: AwtStackable](content: C, useLowPriorityLengths: Boolean = false) = new AlignFrame(content, Center)
	
	/**
	  * Creates a frame that places content to a specified side
	  * @param content Content for the frame
	  * @param side Targeted side
	  * @tparam C Type of frame content
	  * @return A new frame
	  */
	def toSide[C <: AwtStackable](content: C, side: Direction2D) = new AlignFrame(
		content, Alignment forDirection side)
	
	/**
	  * Creates an aligned frame
	  * @param content Content for the frame
	  * @param alignment Alignment used
	  * @tparam C Type of frame content
	  * @return A new frame
	  */
	def apply[C <: AwtStackable](content: C, alignment: Alignment) = new AlignFrame(
		content, alignment)
}

/**
 * A swing component that aligns the underlying component inside a frame
 * @author Mikko Hilpinen
 * @since 3.11.2019, v1+
 */
class AlignFrame[C <: AwtStackable](initialComponent: C, initialAlignment: Alignment)
	extends AlignFrameLike[C] with SwingComponentRelated with AwtContainerRelated with CustomDrawableWrapper with Alignable
{
	// ATTRIBUTES	---------------------
	
	private var _alignment = initialAlignment
	
	private val panel = new Panel[C]
	
	
	// INITIAL CODE	--------------------
	
	set(initialComponent)
	// Updates content layout each time size changes
	addResizeListener(updateLayout())
	
	
	// IMPLEMENTED	---------------------
	
	override def alignment = _alignment
	// Alignment is mutable but component layout must be revalidated each time alignment changes
	def alignment_=(newAlignment: Alignment) =
	{
		if (_alignment != newAlignment)
		{
			_alignment = newAlignment
			revalidate()
		}
	}
	
	override protected def container = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override protected def add(component: C, index: Int) = panel.insert(component, index)
	
	override protected def remove(component: C) = panel -= component
	
	override def align(alignment: Alignment) = this.alignment = alignment
}
