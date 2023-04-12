package utopia.reflection.container.swing.layout.wrapper

import utopia.firmament.context.BaseContextLike
import utopia.firmament.drawing.mutable.MutableCustomDrawableWrapper
import utopia.paradigm.color.Color
import utopia.firmament.drawing.immutable.RoundedBackgroundDrawer
import utopia.reflection.component.drawing.template.DrawLevel.Normal
import utopia.reflection.component.swing.template.{AwtComponentRelated, SwingComponentRelated}
import utopia.reflection.component.template.layout.stack.ReflectionStackable
import utopia.reflection.container.stack.template.layout.ReflectionFramingLike
import utopia.reflection.container.swing.layout.multi.Stack.AwtStackable
import utopia.reflection.container.swing.{AwtContainerRelated, Panel}
import utopia.reflection.shape.stack.{StackInsets, StackSize}

object Framing
{
	/**
	  * Creates a symmetric framing
	  * @param component Component being framed
	  * @param margins Symmetric margins placed around the component on each side (equal margins on both sides).
	  *                The total amount of margins around the component will be twice the amount of these margins
	  * @tparam C Type of wrapped component
	  * @return A framing
	  */
	def symmetric[C <: AwtStackable](component: C, margins: StackSize) =
		new Framing(component, StackInsets.symmetric(margins * 2))
	
	/**
	  * Creates a framing that is filled with both content and color
	  * @param insets Insets around the framing contents
	  * @param color Framing background color
	  * @param f A function for creating frame contents, takes modified frame context as a parameter
	  * @param context Component creation context before modification (implicit)
	  * @tparam C Type of content placed in this frame
	  * @tparam Context2 Type of context in this frame
	  * @tparam Context1 Type of context outside this frame
	  * @return A frame with background color and contents
	  */
	def fill[C <: AwtStackable, Context2, Context1 <: BaseContextLike[_, Context2]]
	(insets: StackInsets, color: Color)(f: Context2 => C)(implicit context: Context1) =
	{
		val newContext = context.against(color)
		val newComponent = f(newContext)
		val framing = new Framing[C](newComponent, insets)
		framing.background = color
		framing
	}
}

/**
  * Framings are containers that present a component with scaling 'frames', like a painting
  * @author Mikko Hilpinen
  * @since 26.4.2019, v1+
  */
class Framing[C <: ReflectionStackable with AwtComponentRelated](initialComponent: C, val insets: StackInsets)
	extends ReflectionFramingLike[C] with SwingComponentRelated with AwtContainerRelated with MutableCustomDrawableWrapper
{
	// ATTRIBUTES	--------------------
	
	private val panel = new Panel[C]()
	private var _content = initialComponent
	
	
	// INITIAL CODE	--------------------
	
	panel += initialComponent
	// Each time Framing size changes, changes content size too
	addResizeListener(updateLayout())
	
	
	// IMPLEMENTED	--------------------
	
	override protected def content: C = _content
	
	override def drawable = panel
	
	override protected def container = panel
	
	override def component = panel.component
	
	override protected def _set(content: C): Unit = {
		panel -= _content
		_content = content
		panel.insert(content, 0)
	}
	
	
	// OTHER	------------------------
	
	/**
	  * Adds rounded background drawing to this framing
	  * @param color Color to use when drawing the background
	  */
	def addRoundedBackgroundDrawing(color: Color) = {
		insets.lengths.map { _.optimal }.filter { _ > 0.0 }.minOption match {
			case Some(minSide) => addCustomDrawer(RoundedBackgroundDrawer.withRadius(color, minSide, Normal))
			case None => addCustomDrawer(RoundedBackgroundDrawer.withFactor(color, 0.25, Normal))
		}
	}
}
