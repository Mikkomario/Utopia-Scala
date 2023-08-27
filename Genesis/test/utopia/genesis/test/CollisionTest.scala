package utopia.genesis.test

import scala.math.Ordering.Double.TotalOrdering
import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.enumeration.Axis._
import utopia.paradigm.generic.ParadigmDataType
import utopia.paradigm.shape.shape2d.area.Circle
import utopia.paradigm.shape.shape2d.line.Line
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point

/**
 * This test makes sure circle and line class projection and collision algorithms are working 
 * properly
 * @author Mikko Hilpinen
 */
object CollisionTest extends App
{
    ParadigmDataType.setup()
    
    val circle1 = Circle(Point.origin, 2)
    val circle2 = Circle(Point(3), 2)
    
    assert(circle1.projectedOver(X) == Line(Point(-2), Point(2)), circle1.projectedOver(X))
    
    val mtv1 = circle1.collisionMtvWith(circle2)
    
    assert(mtv1.isDefined)
    assert(mtv1.get == Vector2D(-1))
    
    val collisionPoints1 = circle1.circleIntersection(circle2).sortBy { _.y }
    
    assert(collisionPoints1.size == 2)
    
    val point1 = collisionPoints1(0)
    val point2 = collisionPoints1(1)
    
    assert(collisionPoints1.forall { _.x == 1.5 })
    assert(point1.y < 0)
    assert(point1.y > -3)
    assert(point2.y > 0)
    assert(point2.y < 3)
    
    val line1 = Line(Point(1.5, -3), Point(1.5, 3))
    
    assert(line1.projectedOver(Y) ~== Line(Point(0, -3), Point(0, 3)), line1.projectedOver(Y))
    assert(line1.projectedOver(X) ~== Line(Point(1.5), Point(1.5)), line1.projectedOver(X))
    assert(line1.collisionAxes.size == 2)
    assert(line1.collisionAxes.exists { _ isParallelWith X })
    
    val mtv2 = circle1.collisionMtvWith(line1, line1.collisionAxes)
    
    assert(mtv2.exists { _ ~== Vector2D(-0.5) }, mtv2)
    
    val collisionPoints2 = line1.circleIntersection(circle1).sortBy { _.y }
    
    println(collisionPoints2)
    assert(collisionPoints2.hasEqualContentWith(collisionPoints1) { _ ~== _ })
    
    println("Success!")
}