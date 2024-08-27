package utopia.reach.test.interactive.drawable

import utopia.flow.collection.immutable.{Empty, Pair}
import utopia.flow.operator.filter.Filter
import utopia.flow.view.immutable.eventful.AlwaysTrue
import utopia.flow.view.mutable.eventful.{EventfulPointer, ResettableFlag}
import utopia.flow.view.template.eventful.{Changing, FlagLike}
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.drawing.AbstractDrawable
import utopia.genesis.handling.event.consume.ConsumeChoice
import utopia.genesis.handling.event.keyboard.Key.Space
import utopia.genesis.handling.event.keyboard.KeyStateEvent.KeyStateEventFilter
import utopia.genesis.handling.event.keyboard.{KeyStateEvent, KeyStateListener, KeyboardEvents}
import utopia.genesis.handling.event.mouse.{MouseButton, MouseButtonStateEvent, MouseButtonStateListener}
import utopia.paradigm.angular.Angle
import utopia.paradigm.color.{Color, Hsl}
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.Polygon
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.reach.test.interactive.drawable.DrawableReachTestContext._
import utopia.reach.test.ReachTestContext._

/**
  * A visual test for splitting polygons to their convex parts
  * @author Mikko Hilpinen
  * @since 25.08.2024, v1.4
  */
object PolygonConvexityTest extends App
{
	// ATTRIBUTES   --------------------
	
	private val cornersPointer = EventfulPointer[Seq[Point]](Empty)
	private val hasCornersFlag: FlagLike = cornersPointer.strongMap { _.nonEmpty }
	private val polygonPointer = cornersPointer.strongMap(Polygon.apply)
	private val polygonCenterPointer = polygonPointer.strongMap { _.center }
	
	private val triangleModeFlag = ResettableFlag()
	private val polygonsPointer = polygonPointer.mergeWith(triangleModeFlag) { (polygon, triangleMode) =>
		if (triangleMode) polygon.toTriangles else polygon.convexParts
	}
	
	
	// APP CODE ------------------------
	
	cornersPointer.addContinuousListener { e => println(s"Now at ${ e.newValue.size } corners") }
	
	display(MouseTracker, PolygonsDrawer)
	KeyboardEvents += KeyTracker
	
	
	// NESTED   ------------------------
	
	private object MouseTracker extends MouseButtonStateListener
	{
		// ATTRIBUTES   -----------------
		
		override val mouseButtonStateEventFilter: Filter[MouseButtonStateEvent] = MouseButtonStateEvent.filter.pressed
		
		
		// IMPLEMENTED  ----------------
		
		override def handleCondition: FlagLike = AlwaysTrue
		
		override def onMouseButtonStateEvent(event: MouseButtonStateEvent): ConsumeChoice = event.button match {
			case MouseButton.Left =>
				println("\n------")
				cornersPointer.update { _ :+ event.position }
			case MouseButton.Right => cornersPointer.value = Empty
			case _ => ()
		}
	}
	
	private object KeyTracker extends KeyStateListener
	{
		// ATTRIBUTES   --------------------
		
		override val keyStateEventFilter: KeyStateEventFilter = KeyStateEvent.filter(Space)
		
		
		// IMPLEMENTED  --------------------
		
		override def handleCondition: FlagLike = hasCornersFlag
		
		override def onKeyState(event: KeyStateEvent): Unit = triangleModeFlag.value = event.pressed
	}
	
	private object PolygonsDrawer extends AbstractDrawable
	{
		// ATTRIBUTES   ----------------
		
		private val centerDs = StrokeSettings(Color.white).toDrawSettings
		
		override val drawBoundsPointer: Changing[Bounds] =
			cornersPointer.strongMap { Bounds.aroundPoints(_).enlarged(2.0) }
		private val drawSettingsPointer = polygonsPointer.strongMap { _.size }.strongMap { colorCount =>
			(0 until colorCount).map { i =>
				val color = Hsl(Angle.circles(i.toDouble / colorCount))
				DrawSettings(color.withAlpha(0.4))(StrokeSettings(color.withAlpha(0.8), 3.0))
			}
		}
		
		
		// INITIAL CODE ----------------
		
		polygonsPointer.addAnyChangeListener { repaint() }
		
		
		// IMPLEMENTED
		override def handleCondition: FlagLike = hasCornersFlag
		
		override def draw(drawer: Drawer, bounds: Bounds): Unit = {
			val drawBounds = this.drawBounds
			def convert(p: Point) = bounds.relativeToAbsolute(drawBounds.relativize(p))
			
			drawer.antialiasing.use { drawer =>
				// Draws the polygons
				val drawnPolygons = polygonsPointer.value.map { _.map(convert) }.zip(drawSettingsPointer.value)
				println(s"Drawing ${ drawnPolygons.size } polygons")
				drawnPolygons.foreach { case (polygon, drawSettings) =>
					implicit val ds: DrawSettings = drawSettings
					polygon.corners.size match {
						case 1 => drawer.draw(Circle(polygon.corners.head, 2.0))
						case 2 => drawer.draw(Line(Pair(polygon.corners.head, polygon.corners(1))))
						case _ => drawer.draw(polygon)
					}
				}
				
				// Draws the center point
				centerDs.use { implicit ds => drawer.draw(Circle(convert(polygonCenterPointer.value), 3)) }
			}
		}
	}
}
