package utopia.genesis.test

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.test.TestContext._
import utopia.flow.time.TimeExtensions._
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.{Actor, Drawable}
import utopia.genesis.util.DefaultSetup
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.color.Color
import utopia.paradigm.path.{BezierPath, CircularPath, CompoundPath, CubicBezier, Path}
import utopia.paradigm.shape.shape2d.{Circle, Line, Point, Size}

import scala.concurrent.duration.FiniteDuration

/**
  * Visual test for bezier paths
  * @author Mikko Hilpinen
  * @since 21.6.2019, v2.1+
  */
object BezierPathTest extends App
{
	// Sets up the program
	val gameWorldSize = Size(800, 600)
	val setup = new DefaultSetup(gameWorldSize, "Key Test")
	
	// Compound bezier path
	val points = Vector(Point(64, 64), Point(128, 32), Point(320, 128), Point(420, 300), Point(640, 400),
		Point(700, 480)/*, Point(400, 300), Point(120, 480)*/)
	val path = BezierPath(points)
	// val path_extra = BezierPath(points)
	
	// Single cubic bezier
	val start = Point(32, 300)
	val end = Point(700, 300)
	val control1 = Point(64, 480)
	val control2 = Point(400, 64)
	val path2 = CubicBezier(start, control1, control2, end)
	
	// Linear compound path
	val points2 = Vector(Point(100, 100), Point(700, 100), Point(700, 500), Point(100, 500), Point(100, 100))
	val path3 = CompoundPath(points2.paired.map { p => Line(p.first, p.second) }.toVector)
	
	// Circular path
	val circle = Circle(gameWorldSize.toPoint / 2, 200)
	val path4 = CircularPath(circle)
	val orange = Color.red.rgb.average(Color.yellow.rgb)
	
	setup.registerObject(new PathDrawer(Vector(start, end), Color.blue))
	setup.registerObjects(new PointDrawer(control1, Color.blue), new PointDrawer(control2, Color.blue))
	setup.registerObject(new MovingObject(path2, Color.blue))
	
	setup.registerObject(new PathDrawer(points2, Color.magenta))
	setup.registerObject(new MovingObject(path3, Color.magenta))
	
	setup.registerObject(new PathDrawer(points, Color.red))
	setup.registerObject(new MovingObject(path, Color.red))
	
	setup.registerObject(new CircleDrawer(circle, orange))
	setup.registerObject(new MovingObject(path4, orange))
	
	// Starts the program
	setup.start()
}

private class MovingObject(val path: Path[Point], val color: Color) extends Drawable with Actor with Handleable
{
	// ATTRIBUTES	-----------------
	
	implicit val ss: StrokeSettings = StrokeSettings.default
	private implicit val ds: DrawSettings = DrawSettings(color)
	
	private var position = path.start
	private var t = 0.0
	
	override def draw(drawer: Drawer) = drawer.draw(Circle(position, 16))
	
	override def act(duration: FiniteDuration) = {
		t = (t + duration.toPreciseSeconds / 5) % 1
		position = path(t)
	}
}

private class PathDrawer(val points: Seq[Point], val color: Color) extends Drawable with Handleable
{
	// ATTRIBUTES	-------------------
	
	private val pointDs = StrokeSettings(color)
	private val pathDs = StrokeSettings(color.withAlpha(0.5))
	
	private val lines = points.paired.map { p => Line(p.first, p.second) }
	
	
	// IMPLEMENTED	-------------------
	
	override def draw(drawer: Drawer) = {
		// Draws the points first
		points.foreach { p => drawer.draw(Circle(p, 4))(pointDs) }
		// Then draws the path
		lines.foreach { drawer.draw(_)(pathDs) }
	}
}

private class PointDrawer(val p: Point, val color: Color) extends Drawable with Handleable
{
	implicit val ds: DrawSettings = StrokeSettings(color)
	
	override def draw(drawer: Drawer) = drawer.draw(Circle(p, 4))
}

private class CircleDrawer(val circle: Circle, val color: Color) extends Drawable with Handleable
{
	implicit val ds: DrawSettings = StrokeSettings(color)
	
	override def draw(drawer: Drawer) = drawer.draw(circle)
}