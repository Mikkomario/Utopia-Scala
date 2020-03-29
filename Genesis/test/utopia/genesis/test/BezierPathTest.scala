package utopia.genesis.test

import utopia.flow.util.CollectionExtensions._
import utopia.flow.util.TimeExtensions._
import utopia.flow.async.ThreadPool
import utopia.genesis.color.Color
import utopia.genesis.handling.{Actor, Drawable}
import utopia.genesis.shape.path.{BezierPath, CircularPath, CompoundPath, CubicBezier, Path}
import utopia.genesis.shape.shape2D.{Circle, Line, Point, Size}
import utopia.genesis.util.{DefaultSetup, Drawer}
import utopia.inception.handling.immutable.Handleable

import scala.concurrent.ExecutionContext
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
	val path3 = CompoundPath(points2.paired.map { p => Line(p._1, p._2) }.toVector)
	
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
	implicit val context: ExecutionContext = new ThreadPool("Test").executionContext
	setup.start()
}

private class MovingObject(val path: Path[Point], val color: Color) extends Drawable with Actor with Handleable
{
	// ATTRIBUTES	-----------------
	
	private var position = path.start
	private var t = 0.0
	
	override def draw(drawer: Drawer) = drawer.withColor(color, Color.black).draw(Circle(position, 16))
	
	override def act(duration: FiniteDuration) =
	{
		t = (t + duration.toPreciseSeconds / 5) % 1
		position = path(t)
	}
}

private class PathDrawer(val points: Seq[Point], val color: Color) extends Drawable with Handleable
{
	// ATTRIBUTES	-------------------
	
	private val lines = points.paired.map { p => Line(p._1, p._2) }
	
	
	// IMPLEMENTED	-------------------
	
	override def draw(drawer: Drawer) =
	{
		// Draws the points first
		val noFill = drawer.noFill
		val pointDrawer = noFill.withEdgeColor(color)
		points.foreach { p => pointDrawer.draw(Circle(p, 4)) }
		// Then draws the path
		val pathDrawer = noFill.withEdgeColor(color.withAlpha(0.5))
		lines.foreach(pathDrawer.draw)
	}
}

private class PointDrawer(val p: Point, val color: Color) extends Drawable with Handleable
{
	override def draw(drawer: Drawer) = drawer.withEdgeColor(color).noFill.draw(Circle(p, 4))
}

private class CircleDrawer(val circle: Circle, val color: Color) extends Drawable with Handleable
{
	override def draw(drawer: Drawer) = drawer.noFill.withEdgeColor(color).draw(circle)
}