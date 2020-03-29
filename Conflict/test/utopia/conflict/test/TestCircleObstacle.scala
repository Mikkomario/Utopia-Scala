package utopia.conflict.test

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable

import scala.collection.immutable.HashSet
import utopia.conflict.test.TestCollisionGroups.Obstacle
import utopia.genesis.handling.Drawable
import utopia.genesis.shape.shape2D.Circle
import utopia.genesis.util.Drawer
import utopia.inception.handling.immutable.Handleable

/**
 * This is a simple circular obstacle used in visual collision tests
 * @author Mikko Hilpinen
 * @since 6.8.2017
 */
class TestCircleObstacle(val circle: Circle) extends Collidable with Drawable with Handleable
{
    // IMPLEMENTED PROPERTIES    ---------------
    
    override val collisionShape = CollisionShape(circle)
    
    override val collisionGroups = HashSet(Obstacle)
    
    
    // IMPLEMENTED METHODS    ------------------
    
    override def draw(drawer: Drawer) = drawer.draw(circle)
}