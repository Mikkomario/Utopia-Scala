package utopia.paradigm.test

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.paradigm.angular.{Angle, Rotation}
import utopia.paradigm.enumeration.Axis.X
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.area.polygon.c4.Parallelogram
import utopia.paradigm.shape.shape2d.area.polygon.c4.bounds.Bounds
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.Size

/**
 * This test is for some intersection methods and other shape (line, circle) specific methods
 */
object ShapeTest extends App
{
    ParadigmDataType.setup()
    
    // Tests bounds corner calculation
    val b1 = Bounds(Point(1, 2), Size(1, 2))
    
    assert(b1.topLeftCorner == Point(1, 2))
    assert(b1.topRightCorner == Point(2, 2))
    assert(b1.bottomRightCorner == Point(2, 4))
    assert(b1.bottomLeftCorner == Point(1, 4))
    assert(b1.topLeft == b1.topLeftCorner)
    assert(b1.topRight == b1.topRightCorner)
    assert(b1.bottomRight == b1.bottomRightCorner)
    assert(b1.bottomLeft == b1.bottomLeftCorner)
    
    val b2 = Bounds(Point(1, 2), Size(-1, -2))
    
    assert(b2.topLeftCorner == Point(1, 2))
    assert(b2.topRightCorner == Point(0, 2))
    assert(b2.bottomRightCorner == Point(0, 0), b2.bottomRightCorner)
    assert(b2.bottomLeftCorner == Point(1, 0), b2.bottomLeftCorner)
    assert(b2.topLeft == b2.bottomRightCorner, b2.topLeft)
    assert(b2.topRight == b2.bottomLeftCorner, b2.topRight)
    assert(b2.bottomRight == b2.topLeftCorner, b2.bottomRight)
    assert(b2.bottomLeft == b2.topRightCorner, b2.bottomLeft)
    
    val line1 = Line(Point.origin, Point(10))
    val line2 = Line(Point(0, 1), Point(2, -1))
    
    // Tests basic line functions
    assert(line1.start == Point.origin)
    assert(line1.end == Point(10, 0))
    
    assert(line1(0) ~== line1.start)
    assert(line1(1) ~== line1.end)
    assert(line2.yForX(0.0) == 1)
    assert(line2.yForX(2.0) == -1)
    assert(line2.yForX(1.0) == 0)
    assert(line2.xForY(0.0) == 1.0)
    assert(line2.xForY(1.0) == 0.0)
    
    val intersection12 = line1 intersection line2
    
    assert(intersection12.isDefined)
    assert(intersection12.get ~== Point(1))
    
    val line5 = Line(Point(1, 2), Point(1, 1))
    assert(line1.intersection(line5, onlyPointsInSegment = false).exists { _ ~== Point(1, 0) })
    assert(line1.intersection(line5).isEmpty, line1.intersection(line5))
    assert(line5.intersection(line1).isEmpty, line5.intersection(line1))
    
    val circle1 = Circle(Point(3), 2)
    
    assert(line1.circleIntersection(circle1, onlyPointsInSegment = false).size == 2)
    assert(line2.circleIntersection(circle1).size == 1)
    assert(line5.circleIntersection(circle1).isEmpty)
    assert(line5.circleIntersection(circle1, onlyPointsInSegment = false).size == 1)
    
    val circle2 = Circle(Point(4), 1)
    
    assert(circle1 contains circle2)
    assert(!circle2.contains(circle1))
    assert(!circle1.contains(Circle(Point(5), 1)))
    
    // Tests line clipping
    assert(line1.clipped(Point(5), Vector2D(1)).get == Line(Point(5), Point(10)))
    assert(line1.clipped(Point(5, 2), Vector2D(-1)).get == Line(Point.origin, Point(5)))
    assert(line1.clipped(Point(-2, -2), Vector2D(-1)).isEmpty)
    assert(line1.clipped(Point(1, 1), Vector2D(1, 1)).get == Line(Point(2), Point(10)))
    
    // Tests topLeft & bottomRight
    val p1 = Point(-30, -5)
    val p2 = Point(20, -30)
    val bounds1 = Bounds(Point.origin, Size(20, 10))
    val bounds2 = Bounds(p1, Size(5, 5))
    
    assert(bounds1.topLeft == Point.origin)
    assert(bounds2.topLeft == p1)
    assert(bounds1.bottomRight == Point(20, 10))
    assert(bounds2.bottomRight == Point(-25, 0))
    
    assert(Point.topLeft(p1, p2) == Point(-30, -30))
    assert(Point.bottomRight(p1, p2) == Point(20, -5))
    
    // Tests bounds combining
    assert(Bounds.between(Point(-30, -5), Point(20, 10)) == Bounds(Point(-30, -5), Size(50, 15)),
        Bounds.between(Point(-30, -5), Point(20, 10)))
    assert(Bounds.around(Vector(bounds1, bounds2)) == Bounds.between(Point(-30, -5), Point(20, 10)),
        Bounds.around(Vector(bounds1, bounds2)))
    
    // Tests some other bounds methods
    assert(bounds1.bottomSlice(5) == Bounds(Point(0, 5), Size(20, 5)))
    assert(!bounds1.overlapsWith(bounds2))
    
    val bounds3 = Bounds(Point(10), Size(20, 10))
    
    assert(bounds3.overlapsWith(bounds1))
    
    // Tests parallelogram & bounds
    assert(bounds1.topLeftCorner == Point.origin)
    assert(bounds2.topLeftCorner == p1)
    assert(bounds1.topEdge == Vector2D(20, 0))
    assert(bounds2.topEdge == Vector2D(5, 0))
    assert(bounds1.rightEdge == Vector2D(0, 10))
    assert(bounds2.rightEdge == Vector2D(0, 5))
    
    val par1 = Parallelogram(Point(-10, -10), Vector2D(20, 0), Vector2D(0, 20))
    
    assert(par1.topRightCorner == Point(10, -10))
    assert(par1.bottomRightCorner == Point(10, 10))
    assert(par1.bottomLeftCorner == Point(-10, 10))
    assert(par1.bounds == Bounds.between(Point(-10, -10), Point(10, 10)))
    
    assert(par1.translated(X(10)) == par1.copy(topLeftCorner = Point(0, -10)))
    
    val par2 = par1 * Matrix2D.rotation(Rotation.clockwise.degrees(45))
    println(par2)
    
    // Tests line lenDir and line translation
    val l1 = Line.lenDir(NumericSpan(2.0, 4.0), Angle.degrees(90))
    
    assert(l1.start ~== Point(0, 2))
    assert(l1.end ~== Point(0, 4))
    
    val l2 = l1.translated(Vector2D(4, 2))
    
    assert(l2.start ~== Point(4, 4), l2)
    assert(l2.end ~== Point(4, 6), l2)
    
    println("Success!")
}