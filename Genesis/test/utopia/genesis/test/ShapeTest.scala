package utopia.genesis.test

import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.{Bounds, Circle, Line, Point, Size, Vector2D}

/**
 * This test is for some intersection methods and other shape (line, circle) specific methods
 */
object ShapeTest extends App
{
    ParadigmDataType.setup()
    
    val line1 = Line(Point.origin, Point(10))
    val line2 = Line(Point(0, 1), Point(2, -1))
    
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
    assert(line1.intersection(line5, onlyPointsInSegment = false).isDefined)
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
    
    // Tests bounds combining
    val bounds1 = Bounds(Point.origin, Size(20, 10))
    val bounds2 = Bounds(Point(-30, -5), Size(5, 5))
    
    assert(Bounds.around(Vector(bounds1, bounds2)) == Bounds.between(Point(-30, -5), Point(20, 10)))
    
    // Tests some other bounds methods
    assert(bounds1.bottomSlice(5) == Bounds(Point(0, 5), Size(20, 5)))
    assert(!bounds1.overlapsWith(bounds2))
    
    val bounds3 = Bounds(Point(10), Size(20, 10))
    
    assert(bounds3.overlapsWith(bounds1))
    
    println("Success!")
}