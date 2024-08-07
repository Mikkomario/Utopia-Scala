package utopia.conflict.collision

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.shape.shape2d.Matrix2D
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape3d.Matrix3D
import utopia.paradigm.transform.Transformable

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
class Collision(val mtv: Vector2D, calculateCollisionPoints: => Seq[Point])
    extends Transformable[Collision] with EqualsBy
{
    // ATTRIBUTES    ---------------------
    
    /**
     * The points where the two collision participants intersect
     */
    lazy val collisionPoints = calculateCollisionPoints
    
    
    // IMPLEMENTED  ----------------------
    
    override def identity: Collision = this
    
    protected override def equalsProperties = Pair(mtv, collisionPoints)
    
    
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