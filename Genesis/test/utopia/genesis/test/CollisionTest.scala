package utopia.genesis.test

import utopia.genesis.generic.GenesisDataType
import utopia.genesis.shape.Vector3D
import utopia.genesis.shape.shape2D.{Circle, Line, Point}
import utopia.genesis.util.Extensions._
import utopia.genesis.shape.Axis._

/**
 * This test makes sure circle and line class projection and collision algorithms are working 
 * properly
 * @author Mikko Hilpinen
 */
object CollisionTest extends App
{
    GenesisDataType.setup()
    
    val circle1 = Circle(Point.origin, 2)
    val circle2 = Circle(Point(3, 0), 2)
    
    assert(circle1.projectedOver(X) == Line(Point(-2, 0), Point(2, 0)))
    
    val mtv1 = circle1.collisionMtvWith(circle2)
    
    assert(mtv1.isDefined)
    assert(mtv1.get == Vector3D(-1))
    
    val collisionPoints1 = circle1.circleIntersection(circle2).sortBy { _.y }
    
    assert(collisionPoints1.size == 2)
    
    val point1 = collisionPoints1(0)
    val point2 = collisionPoints1(1)
    
    println(collisionPoints1)
    
    assert(collisionPoints1.forall { _.x == 1.5 })
    assert(point1.y < 0)
    assert(point1.y > -3)
    assert(point2.y > 0)
    assert(point2.y < 3)
    
    val line1 = Line(Point(1.5, -3), Point(1.5, 3))
    
    assert(line1.projectedOver(Y) == Line(Point(0, -3), Point(0, 3)))
    assert(line1.projectedOver(X) == Line(Point(1.5, 0), Point(1.5, 0)))
    assert(line1.collisionAxes.size == 2)
    assert(line1.collisionAxes.exists { _ isParallelWith X })
    
    val mtv2 = circle1.collisionMtvWith(line1, line1.collisionAxes)
    
    assert(mtv2.isDefined)
    assert(mtv2.get == Vector3D(-0.5))
    
    val collisionPoints2 = line1.circleIntersection(circle1).sortBy { _.y }
    
    println(collisionPoints2)
    assert(collisionPoints2 ~== collisionPoints1)
    
    println("Success!")
}