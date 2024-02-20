package utopia.reach.drawing

import utopia.firmament.drawing.template.CustomDrawer
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.graphics.DrawLevel2.Foreground
import utopia.genesis.graphics.{DrawLevel2, DrawSettings, Drawer}
import utopia.genesis.handling.MouseMoveListener
import utopia.inception.handling.HandlerType
import utopia.paradigm.color.Color
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.component.template.ReachComponentLike

/**
  * Draws the last recorded mouse position. May be useful for testing and/or debugging.
  * @author Mikko Hilpinen
  * @since 15/01/2024, v1.1.1
  * @param componentPointer A pointer that will contain the component for which the mouse-position is tracked
  * @param radius Radius to use when drawing the mouse position as a circle (default = 1.0)
  * @param insideColor Color to use when drawing a mouse coordinate inside the component's bounds (default = red)
  * @param outsideColor Color to use when drawing a mouse coordinate outside the component's bounds
  *                     (default = blue with 20% alpha)
  */
class MousePositionDrawer(componentPointer: Changing[Option[ReachComponentLike]], radius: Double = 1.0,
                          insideColor: Color = Color.red, outsideColor: Color = Color.blue.withAlpha(0.5))
	extends CustomDrawer with MouseMoveListener
{
	// ATTRIBUTES   ----------------------
	
	private val insideDs = DrawSettings.onlyFill(insideColor)
	private val outsideDs = DrawSettings.onlyFill(outsideColor)
	
	private val shape = Circle(Point.origin, radius)
	private var lastMousePosition = Point.origin
	
	
	// INITIAL CODE ----------------------
	
	// Tracks mouse position in the component(s)
	componentPointer.addContinuousListener { event =>
		event.oldValue.foreach { _.mouseMoveHandler -= this }
		event.newValue.foreach { _.mouseMoveHandler += this }
	}
	
	
	// IMPLEMENTED  ----------------------
	
	override def opaque: Boolean = false
	override def drawLevel: DrawLevel2 = Foreground
	
	override def allowsHandlingFrom(handlerType: HandlerType): Boolean = true
	
	override def onMouseMove(event: MouseMoveEvent): Unit = componentPointer.value.foreach { c =>
		val oldP = lastMousePosition
		lastMousePosition = event.relativeTo(c.position).mousePosition
		c.repaintArea(Bounds.between(oldP, lastMousePosition).enlarged(radius * 2.0))
	}
	
	override def draw(drawer: Drawer, bounds: Bounds): Unit = {
		val mousePosition = bounds.position + lastMousePosition
		implicit val ds: DrawSettings = if (bounds.contains(mousePosition)) insideDs else outsideDs
		drawer.translated(mousePosition).draw(shape)
	}
}
