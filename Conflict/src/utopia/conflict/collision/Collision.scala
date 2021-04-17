package utopia.conflict.collision

import utopia.flow.util.Equatable
import utopia.genesis.shape.shape2D.transform.{AffineTransformable, LinearTransformable}
import utopia.genesis.shape.shape2D.{Matrix2D, Point, Vector2D}
import utopia.genesis.shape.shape3D.Matrix3D

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
// TODO: Should accept a lazy set of collision points instead of a call-by-name function
class Collision(val mtv: Vector2D, calculateCollisionPoints: => Vector[Point]) extends LinearTransformable[Collision]
    with AffineTransformable[Collision] with Equatable
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
    
    override def transformedWith(transformation: Matrix2D) = new Collision(transformation(mtv),
        collisionPoints.map { transformation(_).toPoint })
    
    override def transformedWith(transformation: Matrix3D) = new Collision(transformation(mtv).in2D,
        collisionPoints.map { transformation(_).toPoint })
}