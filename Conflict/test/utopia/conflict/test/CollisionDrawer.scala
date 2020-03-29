package utopia.conflict.test

import utopia.genesis.util.Drawer

import utopia.conflict.collision.Collision
import utopia.conflict.handling.{Collidable, CollisionGroup, CollisionListener}
import utopia.genesis.color.Color
import utopia.genesis.handling.Drawable
import utopia.genesis.shape.VectorLike
import utopia.genesis.shape.shape2D.{Circle, Line, Point}
import utopia.genesis.util.DepthRange
import utopia.inception.handling.immutable.Handleable

import scala.concurrent.duration.FiniteDuration

/**
 * A collision drawer draws collision points and mtv data visually on the screen based on the events 
 * it receives
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class CollisionDrawer(target: Collidable, listenGroups: Option[Set[CollisionGroup]] = None) extends Drawable
    with CollisionListener with Handleable
{
    // ATTRIBUTES    ---------------------
    
    private var collisionPoints = Vector[Point]()
    private var mtv = Line.zero
    
    
    // IMPLEMENTED PROPERTIES    --------
    
    override def collisionShape = target.collisionShape
    
    override def targetCollisionGroups = listenGroups
    
    override def drawDepth = DepthRange.foreground
    
    
    // IMPLEMENTED METHODS    -----------
    
    override def draw(drawer: Drawer) = 
    {
        val greenDrawer = drawer.withEdgeColor(Color.green)
        collisionPoints.map { Circle(_, 2) }.foreach(greenDrawer.draw)
        drawer.withEdgeColor(Color.red).draw(mtv)
    }
    
    override def onCollision(collisions: Vector[(Collidable, Collision)], duration: FiniteDuration) =
    {
        println(s"Collides with ${collisions.size} instances")
        
        // Only uses the first collision data
        val collision = collisions.find { _._1 != target }.map { _._2 }
        if (collision.isDefined)
        {
            collisionPoints = collision.get.collisionPoints
            
            if (collisionPoints.isEmpty)
            {
                mtv = Line.zero
            }
            else
            {
                val mtvStart: Point = VectorLike.average(collisionPoints)
                mtv = Line(mtvStart, mtvStart + collision.get.mtv)
            }
        }
    }
}