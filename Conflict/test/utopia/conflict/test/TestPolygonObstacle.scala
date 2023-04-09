package utopia.conflict.test

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable
import utopia.conflict.test.TestCollisionGroups.Obstacle
import utopia.genesis.graphics.{Drawer, StrokeSettings}
import utopia.genesis.handling.Drawable
import utopia.inception.handling.immutable.Handleable
import utopia.paradigm.shape.shape2d.{Line, Polygonic}

import java.awt.Color
import scala.collection.immutable.HashSet

/**
 * These are some obstacles that can be used in the tests
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class TestPolygonObstacle(private val polygon: Polygonic) extends Collidable with Drawable with Handleable
{
    // ATTRIBUTES   -------------------------
    
    private val axeDs = StrokeSettings(Color.ORANGE)
    
    
    // IMPLEMENTED PROPERTIES    --------------------
    
    override val collisionShape = CollisionShape(polygon)
    override val collisionGroups = HashSet(Obstacle)
    
    
    // IMPLEMENTED METHODS    -----------------------
    
    override def draw(drawer: Drawer) =
    {
        drawer.draw(polygon)(StrokeSettings.default)
        val centerPoint = polygon.center
        polygon.collisionAxes.foreach { axis => drawer.draw(Line(centerPoint, centerPoint + axis * 100))(axeDs) }
    }
}