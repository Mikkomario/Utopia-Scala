package utopia.terra.test

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsExtensions._
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.terra.controller.coordinate.map.PointMap2D
import utopia.terra.model.map.MapPoint

/**
  * Tests PointMap2D class
  * @author Mikko Hilpinen
  * @since 02.02.2025, v1.2.2
  */
object PointMapTest extends App
{
	ParadigmDataType.setup()
	
	// The map is an imaginary 200 x 200 square map, where the origin is in the top left corner
	// The vector system's imaginary origin is in the bottom left corner of this map, and the Y-axis goes up instead of down.
	// The scale is also halved, so that the map area is covered with 100 x 100 units of distance
	// private val vectorOrigin = MapPoint(Vector2D.zero, Point(0, 200))
	private val o = MapPoint(Vector2D(0,50), Point(0,100))
	private val ref1 = MapPoint(Vector2D(0, 100), Point.origin)
	private val ref2 = MapPoint(Vector2D(100, 0), Point(200, 200))
	private val projection = new PointMap2D(o, Pair(ref1, ref2))
	
	private val vectors = Matrix2D(Pair(ref1, ref2).map { _.vector - o.vector })
	private val fromVector = vectors.inverse.get
	println(vectors)
	println(fromVector)
	println(fromVector(Vector2D.zero - o.vector))
	println(vectors(fromVector(Vector2D.zero - o.vector)))
	
	assert(projection.pointOnMap(Vector2D.zero) ~== Point(0, 200), projection.pointOnMap(Vector2D.zero))
	assert(projection.pointOnMap(Vector2D(0, 100)) ~== Point.origin)
	assert(projection.pointOnMap(Vector2D(100, 0)) ~== Point(200, 200))
	
	assert(projection.pointOnMap(Vector2D(100,100)) ~== Point(200,0))
	assert(projection.pointOnMap(Vector2D(50,50)) ~== Point(100,100))
	
	println("Success!")
}
