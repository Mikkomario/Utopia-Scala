package utopia.conflict.handling

import utopia.conflict.collision.CollisionShape
import utopia.inception.handling.Handleable

/**
 * Collidable instances can be collided with, they have a specific collision shape
 * @author Mikko Hilpinen
 * @since 2.8.2017
 */
trait Collidable extends Handleable
{
    // ABSTRACT METHODS / PROPERTIES    --------------------------
    
    /**
     * The current shape of the collidable instance.
     */
    def collisionShape: CollisionShape
    
    /**
     * The collision groups this collidable instance should be associated with
     */
    def collisionGroups: Set[CollisionGroup]
}
