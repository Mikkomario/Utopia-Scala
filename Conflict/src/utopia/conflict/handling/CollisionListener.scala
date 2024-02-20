package utopia.conflict.handling

import utopia.conflict.collision.{Collision, CollisionShape}
import utopia.genesis.handling.template.Handleable2
import utopia.inception.handling.Handleable

import scala.concurrent.duration.FiniteDuration

/**
 * Instances implementing CollisionListener trait will continuously be informed about collisions
 * in certain collision groups, provided that the listener is added to a working CollisionHandler.
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
trait CollisionListener extends Handleable2 with HasCollisionShape
{
    // ABSTRACT   ----------------
    
    /**
     * The collision groups the listener is interested in.
      * None if this listener wants to be informed  of collisions in all available groups.
     */
    def targetCollisionGroups: Option[Iterable[CollisionGroup]]
    
    /**
     * This methods is called when a collision is recognised between this listener and another instance
     * @param collisions the collisions that took place, each with the associated instance
     * @param duration the time duration since the last collision check
     */
    def onCollision(collisions: Iterable[(CanCollideWith, Collision)], duration: FiniteDuration): Unit
}
