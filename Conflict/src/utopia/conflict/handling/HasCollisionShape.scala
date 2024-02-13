package utopia.conflict.handling

import utopia.conflict.collision.CollisionShape
import utopia.flow.view.template.eventful.Changing
import utopia.genesis.handling.template.Handleable2

/**
  * Common trait for items that have a specific collision shape
  * @author Mikko Hilpinen
  * @since 2.8.2017
  */
trait HasCollisionShape
{
    // ABSTRACT     --------------------------
	
	/**
	  * @return A pointer that contains this item's current collision shape
	  */
	def collisionShapePointer: Changing[CollisionShape]
	
	
	// COMPUTED ----------------------------
	
	/**
	  * The current shape of this instance.
	  */
	def collisionShape: CollisionShape = collisionShapePointer.value
}
