package utopia.conflict.handling

import utopia.conflict.collision.{Collision, CollisionShape}
import utopia.inception.handling.Handleable

import scala.concurrent.duration.FiniteDuration

/**
 * Instances implementing CollisionListener trait will continuously be informed about collisions
 * in certain collision groups, provided that the listener is added to a working CollisionHandler.
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
trait CollisionListener extends Handleable
{
    // ABSTRACT METHODS & PROPERTIES    ----------------
    
    /**
     * The shape against which the collisions are checked
     */
    def collisionShape: CollisionShape
    
    /**
     * The collision groups the listener is interested in. None if the listener wants to be informed
     * of collisions in all available groups.
     */
    def targetCollisionGroups: Option[Traversable[CollisionGroup]]
    
    /**
     * This methods is called when a collision is recognised between the listener and another
     * collidable instance
     * @param collisions the collisions that took place, each with the associated collidable instance
     * @param duration the time duration since the last collision check
     */
    def onCollision(collisions: Vector[(Collidable, Collision)], duration: FiniteDuration)
}
