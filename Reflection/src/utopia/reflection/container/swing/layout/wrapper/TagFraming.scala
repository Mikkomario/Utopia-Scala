package utopia.reflection.container.swing.layout.wrapper

import utopia.flow.datastructure.mutable.{PointerWithEvents, ResettableLazy}
import utopia.flow.event.Changing
import utopia.genesis.color.Color
import utopia.reflection.component.drawing.mutable.CustomDrawableWrapper
import utopia.reflection.component.drawing.view.RoundedBackgroundViewDrawer
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.container.stack.template.layout.FramingLike
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.shape.stack.StackInsets

/**
  * Used for wrapping a component and for drawing a rounded "tag" background on it
  * @author Mikko Hilpinen
  * @since 27.9.2020, v1.3
  */
class TagFraming[C <: AwtStackable](initialComponent: C, initialColor: Color) extends FramingLike[C]
	with SwingComponentRelated with AwtContainerRelated with CustomDrawableWrapper
{
	// ATTRIBUTES   --------------------------
	
	private val panel = new Panel[C]()
	private val cachedInsets = ResettableLazy { calculateInsets() }
	
	/**
	  * Pointer that contains the current tag background color
	  */
	val colorPointer = new PointerWithEvents(initialColor)
	
	
	// INITIAL CODE --------------------------
	
	set(initialComponent)
	// Each time Framing size changes, changes content size too
	addResizeListener(updateLayout())
	// Adds rounded background drawing
	addCustomDrawer(RoundedBackgroundViewDrawer.withFactor(colorPointer, Changing.wrap(1.0)))
	
	colorPointer.addListener { _ => repaint() }
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return This tag's current background color
	  */
	def color = colorPointer.value
	def color_=(newColor: Color) = colorPointer.value = newColor
	
	
	// IMPLEMENTED  --------------------------
	
	override def insets = cachedInsets.value
	
	override protected def container = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override protected def add(component: C, index: Int) = panel.insert(component, index)
	
	override protected def remove(component: C) = panel -= component
	
	override def resetCachedSize() =
	{
		cachedInsets.reset()
		super.resetCachedSize()
	}
	
	
	// OTHER    ------------------------------
	
	private def calculateInsets() = content match
	{
		case Some(c) =>
			val contentSize = c.stackSize
			val (shorterAxis, shorterLength) = contentSize.toMap2D.minBy { _._2.optimal }
			// Insets are applied to the longer side and their length is 1/2 of the shorter side length
			StackInsets.symmetric(shorterLength / 2, shorterAxis.perpendicular)
		case None => StackInsets.any
	}
}
