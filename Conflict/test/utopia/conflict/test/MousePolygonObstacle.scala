package utopia.conflict.test

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable

import scala.collection.immutable.HashSet
import utopia.conflict.test.TestCollisionGroups.UserInput
import utopia.genesis.util.Drawer
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.handling.{Drawable, MouseMoveListener}
import utopia.paradigm.transform.AffineTransformation
import utopia.paradigm.shape.shape2d.Polygonic
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
    private var currentTransformation = AffineTransformation.identity
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    override def collisionShape = relativeCollisionShape * currentTransformation
    
    override def collisionGroups = HashSet(UserInput)
    
    
    // IMPLEMENTED METHODS    --------
    
    override def draw(drawer: Drawer) = drawer.transformed(currentTransformation).draw(relativePolygon)
    
    override def onMouseMove(event: MouseMoveEvent) = currentTransformation = AffineTransformation.translation(
        event.mousePosition.toVector)
}