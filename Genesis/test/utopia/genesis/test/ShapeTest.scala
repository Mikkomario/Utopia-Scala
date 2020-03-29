package utopia.genesis.test

import utopia.genesis.shape.Vector3D
import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.shape2D.{Bounds, Circle, Line, Point, Size}

/**
 * This test is for some intersection methods and other shape (line, circle) specific methods
 */
object ShapeTest extends App
{
    GenesisDataType.setup()
    
    val line1 = Line(Point.origin, Point(10, 0))
    val line2 = Line(Point(0, 1), Point(2, -1))
    
    assert(line1(0) ~== line1.start)
    assert(line1(1) ~== line1.end)
    
    val intersection12 = line1 intersection line2
    
    assert(intersection12.isDefined)
    assert(intersection12.get ~== Point(1, 0))
    
    val line5 = Line(Point(1, 2), Point(1, 1))
    assert(line1.intersection(line5, false).isDefined)
    assert(line1.intersection(line5).isEmpty)
    assert(line5.intersection(line1).isEmpty)
    
    val circle1 = Circle(Point(3, 0), 2)
    
    assert(line1.circleIntersection(circle1, false).size == 2)
    assert(line2.circleIntersection(circle1).size == 1)
    assert(line5.circleIntersection(circle1).isEmpty)
    assert(line5.circleIntersection(circle1, false).size == 1)
    
    val circle2 = Circle(Point(4, 0), 1)
    
    assert(circle1 contains circle2)
    assert(!circle2.contains(circle1))
    assert(!circle1.contains(Circle(Point(5, 0), 1)))
    
    // Tests line clipping
    assert(line1.clipped(Point(5, 0), Vector3D(1)).get == Line(Point(5, 0), Point(10, 0)))
    assert(line1.clipped(Point(5, 2), Vector3D(-1)).get == Line(Point.origin, Point(5, 0)))
    assert(line1.clipped(Point(-2, -2), Vector3D(-1)).isEmpty)
    assert(line1.clipped(Point(1, 1), Vector3D(1, 1)).get == Line(Point(2, 0), Point(10, 0)))
    
    // Tests bounds combining
    val bounds1 = Bounds(Point.origin, Size(20, 10))
    val bounds2 = Bounds(Point(-30, -5), Size(5, 5))
    
    assert(Bounds.around(Vector(bounds1, bounds2)) == Bounds.between(Point(-30, -5), Point(20, 10)))
    
    // Tests some other bounds methods
    assert(bounds1.bottomSlice(5) == Bounds(Point(0, 5), Size(20, 5)))
    
    println("Success!")
}