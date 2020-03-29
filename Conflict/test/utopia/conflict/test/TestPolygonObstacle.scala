package utopia.conflict.test

import scala.collection.immutable.HashSet
import utopia.conflict.test.TestCollisionGroups.Obstacle
import utopia.genesis.util.Drawer
import java.awt.Color

import utopia.conflict.collision.CollisionShape
import utopia.conflict.handling.Collidable
import utopia.genesis.handling.Drawable
import utopia.genesis.shape.shape2D.{Line, Polygonic}
import utopia.inception.handling.immutable.Handleable

/**
 * These are some obstacles that can be used in the tests
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class TestPolygonObstacle(private val polygon: Polygonic) extends Collidable with Drawable with Handleable
{
    // IMPLEMENTED PROPERTIES    --------------------
    
    override val collisionShape = CollisionShape(polygon)
    
    override val collisionGroups = HashSet(Obstacle)
    
    
    // IMPLEMENTED METHODS    -----------------------
    
    override def draw(drawer: Drawer) = 
    {
        drawer.draw(polygon)
        val orangeDrawer = drawer.withEdgeColor(Color.ORANGE)
        val centerPoint = polygon.center
        polygon.collisionAxes.foreach { axis => orangeDrawer.draw(Line(centerPoint, centerPoint + axis * 100)) }
    }
}