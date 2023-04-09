package utopia.conflict.test

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable
import utopia.conflict.test.TestCollisionGroups.UserInput
import utopia.genesis.event.MouseMoveEvent
import utopia.genesis.graphics.{DrawSettings, Drawer, StrokeSettings}
import utopia.genesis.handling.{Drawable, MouseMoveListener}
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.shape.shape2d.Polygonic
import utopia.paradigm.transform.AffineTransformation

import scala.collection.immutable.HashSet

/**
 * This polygon-shaped obstacle moves along with the mouse
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class MousePolygonObstacle(private val relativePolygon: Polygonic) extends Collidable with Drawable
    with MouseMoveListener with Handleable
{
    // ATTRIBUTES    ------------------
    
    private implicit val ds: DrawSettings = StrokeSettings.default
    
    private val relativeCollisionShape = CollisionShape(relativePolygon)
    private var currentTransformation = AffineTransformation.identity
    
    
    // IMPLEMENTED PROPERTIES    -----
    
    override def collisionShape = relativeCollisionShape * currentTransformation
    
    override def collisionGroups = HashSet(UserInput)
    
    
    // IMPLEMENTED METHODS    --------
    
    override def draw(drawer: Drawer) = currentTransformation(drawer).draw(relativePolygon)
    
    override def onMouseMove(event: MouseMoveEvent) = currentTransformation = AffineTransformation.translation(
        event.mousePosition.toVector)
}