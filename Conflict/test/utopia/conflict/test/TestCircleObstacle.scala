package utopia.conflict.test

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable
import utopia.conflict.test.TestCollisionGroups.Obstacle
import utopia.genesis.graphics.{Drawer3, StrokeSettings}
import utopia.genesis.handling.Drawable2
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.shape.shape2d.Circle

import scala.collection.immutable.HashSet

/**
 * This is a simple circular obstacle used in visual collision tests
 * @author Mikko Hilpinen
 * @since 6.8.2017
 */
class TestCircleObstacle(val circle: Circle) extends Collidable with Drawable2 with Handleable
{
    // IMPLEMENTED PROPERTIES    ---------------
    
    override val collisionShape = CollisionShape(circle)
    
    override val collisionGroups = HashSet(Obstacle)
    
    
    // IMPLEMENTED METHODS    ------------------
    
    override def draw(drawer: Drawer3) = drawer.draw(circle)(StrokeSettings.default)
}