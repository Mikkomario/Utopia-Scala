package utopia.conflict.test

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable

import scala.collection.immutable.HashSet
import utopia.conflict.test.TestCollisionGroups.UserInput
import utopia.genesis.util.Drawer
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.handling.{Drawable, MouseMoveListener}
import utopia.genesis.shape.shape2D.{Polygonic, Transformation}
import utopia.inception.handling.immutable.Handleable

/**
 * This polygon-shaped obstacle moves along with the mouse
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class MousePolygonObstacle(private val relativePolygon: Polygonic) extends Collidable with Drawable
    with MouseMoveListener with Handleable
{
    // ATTRIBUTES    ------------------
    
    private val relativeCollisionShape = CollisionShape(relativePolygon)
    private var currentTransformation = Transformation.identity
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    override def collisionShape = currentTransformation.toAbsolute(relativeCollisionShape)
    
    override def collisionGroups = HashSet(UserInput)
    
    
    // IMPLEMENTED METHODS    --------
    
    override def draw(drawer: Drawer) = drawer.transformed(currentTransformation).draw(relativePolygon)
    
    override def onMouseMove(event: MouseMoveEvent) = currentTransformation = Transformation.translation(
        event.mousePosition.toVector)
}