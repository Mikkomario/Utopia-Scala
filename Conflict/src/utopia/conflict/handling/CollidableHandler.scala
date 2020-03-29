package utopia.conflict.handling

import utopia.conflict.collision.CollisionShape
import utopia.inception.handling.HandlerType
import utopia.inception.handling.Handler

case object CollidableHandlerType extends HandlerType
{
    override def supportedClass = classOf[Collidable]
}

/**
 * A collidableHandler is used for managing and checking collisions against multiple collidable 
 * instances. The collidables are grouped in specific subgroups so that collision checks can be
 * more specific.
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
trait CollidableHandler extends Handler[Collidable]
{
    override def handlerType = CollidableHandlerType
    
    /**
     * Finds all collisions (in the specified collision groups) for the specified collision shape
     * @param shape the shape against which the collisions are checked
     * @param limitToGroups None if all of the collidable instance should be checked. A collection 
     * of checked collision groups otherwise
     * @return All collisions in the checked groups along with their collidable participants
     */
    def checkForCollisions(shape: CollisionShape, limitToGroups: Option[Traversable[CollisionGroup]] = None) = 
    {
        // TODO: Consider whether this should return view or vector
        handleView().flatMap
        {
            collidable =>
    
                if (limitToGroups.forall { _.exists { collidable.collisionGroups.contains(_) } })
                    shape.checkCollisionWith(collidable.collisionShape).map { collidable -> _ }
                else
                    None
        }.toVector
    }
}