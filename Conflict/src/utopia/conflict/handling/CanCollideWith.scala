package utopia.conflict.handling

import utopia.genesis.handling.template.Handleable

/**
  * Common trait for items that can be collided with, having a specific collision shape
  * @author Mikko Hilpinen
  * @since 2.8.2017
  */
trait CanCollideWith extends Handleable with HasCollisionShape
{
    // ABSTRACT     --------------------------
	
    /**
      * The collision groups this instance is associated with.
      */
    def collisionGroups: Set[CollisionGroup]
}
