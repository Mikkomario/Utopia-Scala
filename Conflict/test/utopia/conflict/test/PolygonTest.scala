package utopia.conflict.test

import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.enumeration.Axis._
import utopia.conflict.collision.Extensions._
import utopia.paradigm.enumeration.RotationDirection.{Clockwise, Counterclockwise}
import utopia.paradigm.shape.shape2d.area.polygon.Polygon
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
 * This test tests the basic polygon features
 * @author Mikko Hilpinen
 * @since 6.7.2017
 */
object PolygonTest extends App
{
    ParadigmDataType.setup()
    
    // Square
    val polygon = Polygon(Vector(Point.origin, Point(3), Point(3, 3), Point(0, 3)))
    
    // Tests basic vertex and edge accessing
    assert(polygon.corners.size == 4)
    assert(polygon.vertex(1) == Point(3))
    assert(polygon.vertex(4) == polygon.vertex(0))
    assert(polygon.vertex(-1) == polygon.vertex(3))
    assert(polygon.side(0) == Line(Point.origin, Point(3)))
    assert(polygon.side(3) == Line(Point(0, 3), Point.origin))
    assert(polygon.sides.size == 4)
    
    // Tests other computed properties
    assert(polygon.bounds.topLeft == Point.origin)
    assert(polygon.bounds.bottomRight == Point(3, 3))
    
    assert(polygon.rotationDirection == Clockwise)
    assert(polygon.isConvex)
    assert(polygon.collisionAxes.size == 2)
    assert(polygon.collisionAxes.exists { _ isParallelWith X })
	assert(polygon.collisionAxes.exists { _ isParallelWith Y })
    assert(polygon.convexParts.contains(polygon))
    
    assert(polygon.center == Point(1.5, 1.5))
    // NB: This assertion may have changed since .circleInside logic was altered in v1.6.1
    assert(polygon.circleAround == polygon.circleInside)
    assert(polygon.circleAround.contains(Point.origin))
    
    // Tests polygon splitting
    val (part1, part2) = polygon.cutBetween(0, 2).toTuple
    
    assert(part1.corners.size == 3)
	assert(part2.corners.size == 3)
    assert(part1 != part2)
    
    assert(polygon.projectedOver(X) == Line(Point.origin, Point(3)))
    assert(polygon.projectedOver(Y) == Line(Point.origin, Point(0, 3)))
    
    assert(polygon.containsProjection(Point(0.3, 0.1), X.unit.toVector2D))
    assert(polygon.containsProjection(Point(0.3, -5), X.unit.toVector2D))
    assert(polygon.containsProjection(Point(0.3, 0.1), Y.unit.toVector2D))
    assert(!polygon.containsProjection(Point(4, 4), X.unit.toVector2D))
    
    // Tests containment
    assert(polygon.contains(Point(0.3, 0.1)))
    assert(polygon.contains(Point(1.5, 1.5)))
    assert(polygon.contains(Point.origin))
	assert(!polygon.contains(Point(-0.4, 2)))
	assert(!polygon.contains(Point(1.5, 4)))
    
    // Sand glass
    val polygon2 = Polygon(Vector(Point.origin, Point(0.5, 1), Point(0, 2), Point(2, 2), Point(1.5, 1), Point(2)))
    
    assert(polygon2.rotationDirection == Counterclockwise)
    assert(!polygon2.isConvex)
    
    val parts2 = polygon2.convexParts // polygon2.cutBetween(1, 4)
    
    assert(parts2.size == 2)
    assert(parts2.forall { _.isConvex })
    assert(parts2.forall { _.corners.size == 4 })
    
    assert(polygon.projectedOver(X) == Line(Point.origin, Point(3)))
    assert(polygon.projectedOver(Y) == Line(Point.origin, Point(0, 3)))
    
    // Tests collision recognition
    val outsideBox = Bounds(Point(0, -2), Size(1, 1))
    
    assert(polygon.checkCollisionWith(outsideBox).isEmpty)
    
    val overlappingBox = Bounds(Point(1, -1), Size(1, 2))
    val collision1 = overlappingBox.checkCollisionWith(polygon)
    
    assert(collision1.isDefined)
    assert(collision1.get.mtv == Vector2D(0, -1))
    
    println(collision1.get.collisionPoints)
    
    val boxInside = Bounds(Point(1.5, 1), Size(1, 1))
    val collision2 = boxInside.checkCollisionWith(polygon)
    
    assert(collision2.isDefined)
    assert(collision2.get.mtv == Vector2D(1.5))
    
    println(collision2.get.collisionPoints)
    
    println("Success!")
}