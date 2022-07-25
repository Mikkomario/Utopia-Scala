package utopia.genesis.test

import utopia.flow.test.TestContext._
import utopia.genesis.color.Color
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.handling.Drawable
import utopia.genesis.shape.shape1D.Rotation
import utopia.genesis.shape.shape2D.{Bounds, Circle, Matrix2D, Parallelogramic, Point, ShapeConvertible, Size}
import utopia.genesis.shape.shape2D.transform.{AffineTransformation, Transformable}
import utopia.genesis.shape.shape3D.Matrix3D
import utopia.genesis.util.{DefaultSetup, Drawer}
import utopia.inception.handling.immutable.Handleable

/**
  * Used for testing use of transformations in drawing
  * @author Mikko Hilpinen
  * @since 29.12.2020, v2.4
  */
object TransformationDrawTest extends App
{
	GenesisDataType.setup()
	
	val gameWorldSize = Size(700, 200)
	val setup = new DefaultSetup(gameWorldSize, "Transformation Test")
	
	setup.registerObjects(
		new ShapeDrawer[Parallelogramic](Bounds(Point(-32, -32), Size(64, 64)), Point.origin, Point(100, 100), Matrix2D.identity),
		new ShapeDrawer[Parallelogramic](Bounds(Point.origin, Size(64, 64)), Point(32, 32), Point(200, 100), Matrix2D.identity),
		new ShapeDrawer[Parallelogramic](Bounds(Point(-32, -32), Size(64, 64)), Point.origin, Point(300, 100),
			Matrix2D.rotation(Rotation.ofDegrees(45))),
		new ShapeDrawer[Parallelogramic](Bounds(Point.origin, Size(64, 64)), Point(32, 32), Point(400, 100),
			Matrix2D.rotation(Rotation.ofDegrees(45))),
		new ShapeDrawer[Parallelogramic](Bounds(Point(-32, -32), Size(64, 64)), Point.origin, Point(500, 100),
			Matrix2D.scaling(2, 0.5) * Matrix2D.rotation(Rotation.ofDegrees(45))),
		new ShapeDrawer[Parallelogramic](Bounds(Point.origin, Size(64, 64)), Point(32, 32), Point(600, 100),
			Matrix2D.scaling(2, 0.5) * Matrix2D.rotation(Rotation.ofDegrees(45)))
	)
	
	setup.start()
}

private class ShapeDrawer[A <: Transformable[A] with ShapeConvertible](shape: A, origin: Point, position: Point,
																	   transformation: Matrix2D)
	extends Drawable with Handleable
{
	// ATTRIBUTES	--------------------------
	
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
	
	override def draw(drawer: Drawer) =
	{
		// Draws the transformed shape and origin
		drawer.onlyFill(Color.blue.withAlpha(0.33)).draw(transformedShape)
		drawer.onlyFill(Color.cyan).draw(transformedOrigin)
		// Performs the same transformations inside the drawer, then draws
		(drawer * combinedTransformation).withStroke(2).disposeAfter { d =>
			d.onlyEdges(Color.magenta).draw(shape)
			d.onlyEdges(Color.red).draw(Circle(origin, 3))
		}
	}
}
