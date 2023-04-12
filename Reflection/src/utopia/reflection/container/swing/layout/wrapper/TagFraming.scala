package utopia.reflection.container.swing.layout.wrapper

import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.caching.ResettableLazy
import utopia.flow.view.mutable.eventful.PointerWithEvents
import utopia.paradigm.color.Color
import utopia.firmament.drawing.view.RoundedBackgroundViewDrawer
import utopia.reflection.component.swing.template.SwingComponentRelated
import utopia.reflection.container.stack.template.layout.ReflectionFramingLike
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.shape.stack.StackInsets

/**
  * Used for wrapping a component and for drawing a rounded "tag" background on it
  * @author Mikko Hilpinen
  * @since 27.9.2020, v1.3
  */
class TagFraming[C <: AwtStackable](initialComponent: C, initialColor: Color)
	extends ReflectionFramingLike[C] with SwingComponentRelated with AwtContainerRelated with MutableCustomDrawableWrapper
{
	// ATTRIBUTES   --------------------------
	
	private val panel = new Panel[C]()
	private val cachedInsets = ResettableLazy { calculateInsets() }
	
	/**
	  * Pointer that contains the current tag background color
	  */
	val colorPointer = new PointerWithEvents(initialColor)
	
	private var _content = initialComponent
	
	
	// INITIAL CODE --------------------------
	
	panel += initialComponent
	// Each time Framing size changes, changes content size too
	addResizeListener(updateLayout())
	// Adds rounded background drawing
	addCustomDrawer(RoundedBackgroundViewDrawer.withFactor(colorPointer, Fixed(1.0)))
	
	colorPointer.addContinuousAnyChangeListener { repaint() }
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return This tag's current background color
	  */
	def color = colorPointer.value
	def color_=(newColor: Color) = colorPointer.value = newColor
	
	
	// IMPLEMENTED  --------------------------
	
	override protected def content: C = _content
	
	override def insets = cachedInsets.value
	
	override protected def container = panel
	
	override def component = panel.component
	
	override def drawable = panel
	
	override protected def _set(content: C): Unit = {
		panel -= _content
		_content = content
		panel.insert(content, 0)
	}
	
	override def resetCachedSize() = {
		cachedInsets.reset()
		super.resetCachedSize()
	}
	
	
	// OTHER    ------------------------------
	
	private def calculateInsets() = {
		val contentSize = content.stackSize
		// TODO: Could use .components here (but not yet added)
		val (shorterLength, shorterAxis) = contentSize.dimensions.zipWithAxis2D.minBy { _._1.optimal }
		// Insets are applied to the longer side and their length is 1/2 of the shorter side length
		StackInsets.symmetric(shorterLength / 2, shorterAxis.perpendicular)
	}
}
