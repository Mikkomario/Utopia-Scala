package utopia.conflict.collision

import utopia.flow.util.Equatable
import utopia.genesis.shape.Vector3D
import utopia.genesis.shape.shape2D.{Point, TransformProjectable, Transformation}

/**
 * Collision instances contain information about a collision event
 * @author Mikko Hilpinen
 * @since 13.7.2017
 * @param mtv The minimum translation vector for the primary collision participant. A minimum translation 
 * vector defines the smallest possible translation that gets the participant out of the 
 * collision situation
 * @param calculateCollisionPoints This function returns the collision / intersection points 
 * involved in the collision event. The function is called when the points are requested for the 
 * first time
 */
class Collision(val mtv: Vector3D, calculateCollisionPoints: => Vector[Point]) extends TransformProjectable[Collision]
    with Equatable
{
    // ATTRIBUTES    ---------------------
    
    /**
     * The points where the two collision participants intersect
     */
    lazy val collisionPoints = calculateCollisionPoints
    
    
    // IMPLEMENTED  ----------------------
    
    override def properties = Vector(mtv, collisionPoints)
    
    
    // OPERATORS    ----------------------
    
    /**
     * This collision from the opposite point of view
     */
    def unary_- = new Collision(-mtv, collisionPoints)
    
    /**
     * Combines two collision information instances
     */
    def +(other: Collision) = new Collision(mtv + other.mtv, (collisionPoints ++ other.collisionPoints).distinct)
    
    
    // IMPLEMENTED METHODS    ------------
    
    override def transformedWith(transformation: Transformation) = new Collision(
            transformation(mtv), collisionPoints.map { transformation(_) })
}