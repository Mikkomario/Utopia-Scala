package utopia.genesis.test.interactive

import utopia.flow.test.TestContext._
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.Drawable
import utopia.genesis.util.DefaultSetup
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.angular.Rotation
import utopia.paradigm.color.Color
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d._
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.Parallelogramic
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.transform.{AffineTransformation, Transformable}

/**
  * Used for testing use of transformations in drawing
  * @author Mikko Hilpinen
  * @since 29.12.2020, v2.4
  */
object TransformationDrawTest extends App
{
	ParadigmDataType.setup()
	
	val gameWorldSize = Size(700, 200)
	val setup = new DefaultSetup(gameWorldSize, "Transformation Test")
	
	setup.registerObjects(
		new ShapeDrawer[Parallelogramic](Bounds(Point(-32, -32), Size(64, 64)), Point.origin, Point(100, 100), Matrix2D.identity),
		new ShapeDrawer[Parallelogramic](Bounds(Point.origin, Size(64, 64)), Point(32, 32), Point(200, 100), Matrix2D.identity),
		new ShapeDrawer[Parallelogramic](Bounds(Point(-32, -32), Size(64, 64)), Point.origin, Point(300, 100),
			Matrix2D.rotation(Rotation.clockwise.degrees(45))),
		new ShapeDrawer[Parallelogramic](Bounds(Point.origin, Size(64, 64)), Point(32, 32), Point(400, 100),
			Matrix2D.rotation(Rotation.clockwise.degrees(45))),
		new ShapeDrawer[Parallelogramic](Bounds(Point(-32, -32), Size(64, 64)), Point.origin, Point(500, 100),
			Matrix2D.scaling(2, 0.5) * Matrix2D.rotation(Rotation.clockwise.degrees(45))),
		new ShapeDrawer[Parallelogramic](Bounds(Point.origin, Size(64, 64)), Point(32, 32), Point(600, 100),
			Matrix2D.scaling(2, 0.5) * Matrix2D.rotation(Rotation.clockwise.degrees(45)))
	)
	
	setup.start()
}

private class ShapeDrawer[A <: Transformable[A] with ShapeConvertible](shape: A, origin: Point, position: Point,
																	   transformation: Matrix2D)
	extends Drawable with Handleable
{
	// ATTRIBUTES	--------------------------
	
	private implicit val ss: StrokeSettings = StrokeSettings.default
	private val broadSs = StrokeSettings(strokeWidth = 2.0)
	
	private val transformedDs = DrawSettings(Color.blue.withAlpha(0.33))
	private val originDs = DrawSettings(Color.cyan)
	private val comboDs = DrawSettings(Color.magenta)(broadSs)
	private val comboOriginDs = DrawSettings(Color.red)(broadSs)
	
	private val combinedTransformation = (AffineTransformation.translation(-origin.toVector) * transformation).translated(position)
	private val transformedShape = shape * combinedTransformation// (shape.translated(-origin) * transformation).translated(position)
	private val transformedOrigin = Circle(origin * combinedTransformation, 2)// Circle((origin.translated(-origin) * transformation).translated(position), 2)
	
	
	// INITIAL CODE	--------------------------
	
	{
		println(s"\nDrawing $shape with origin at $origin to $position with transformation: $transformation")
		val originZero = shape.translated(-origin)
		println(s"1) Translated so that origin is at (0,0): $originZero")
		val transformed = originZero * transformation
		println(s"2) Transformed: $transformed")
		val positioned = transformed.translated(position)
		println(s"3) Positioned: $positioned")
		
		println(s"\nSame as transformation")
		val originTranslation = Matrix3D.translation(-origin)
		println(s"1) Translation so that origin is at (0,0): $originTranslation")
		val transformationState = originTranslation * transformation
		println(s"2) Transformation ($transformation): $transformationState")
		val positionTranslation = Matrix3D.translation(position)
		println(s"3) Positioning ($positionTranslation): ${transformationState * positionTranslation} (should be $combinedTransformation)")
	}
	
	
	// IMPLEMENTED	--------------------------
	
	override def draw(drawer: Drawer) = {
		// Draws the transformed shape and origin
		drawer.draw(transformedShape)(transformedDs)
		drawer.draw(transformedOrigin)(originDs)
		// Performs the same transformations inside the drawer, then draws
		(drawer * combinedTransformation).use { drawer =>
			drawer.draw(shape)(comboDs)
			drawer.draw(Circle(origin, 3))(comboOriginDs)
		}
	}
}
