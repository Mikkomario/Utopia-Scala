package utopia.conflict.handling

import utopia.genesis.handling.Actor
import utopia.inception.handling.HandlerType
import utopia.inception.handling.Handler

import scala.concurrent.duration.FiniteDuration

case object CollisionHandlerType extends HandlerType
{
    override def supportedClass = classOf[CollisionListener]
}

/**
 * A collision handler handles collision checking between collisionListeners and collidable 
 * instances. A collisionHandler needs to be added to a working ActorHandler in order to function 
 * properly.
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
trait CollisionHandler extends Handler[CollisionListener] with Actor
{
    // ABSTRACT --------------------------------
    
    /**
      * @return The handler which keeps track of collidable instances against which collisions are checked
      */
    def collidableHandler: CollidableHandler
    
    
    // IMPLEMENTED METHODS    ------------------
    
    override def handlerType = CollisionHandlerType
    
    override def act(duration: FiniteDuration) =
    {
        // Checks collisions for each listener
        handle
        {
            listener =>
                // Doesn't include collisions with listener itself
                val collisions = collidableHandler.checkForCollisions(listener.collisionShape,
                    listener.targetCollisionGroups).filterNot { _._1 == listener }
                if (collisions.nonEmpty)
                    listener.onCollision(collisions, duration)
        }
    }
}