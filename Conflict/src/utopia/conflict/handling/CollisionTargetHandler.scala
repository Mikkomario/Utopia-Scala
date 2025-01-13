package utopia.conflict.handling

import utopia.conflict.collision.CollisionShape
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.util.logging.Logger
import utopia.genesis.handling.template.Handleable

import scala.annotation.unused

object CollisionTargetHandler
{
    // COMPUTED ----------------------------
    
    def factory(implicit log: Logger) = new CollisionTargetHandlerFactory()
    
    
    // IMPLICIT ----------------------------
    
    implicit def objectToFactory(@unused o: CollisionTargetHandler.type)
                                (implicit log: Logger): CollisionTargetHandlerFactory =
        factory
    
    
    // OTHER    ----------------------------
    
    /**
      * @param items Initially assigned collision targets
      * @return A collision target handler
      */
    def apply(items: IterableOnce[CanCollideWith])(implicit log: Logger) = new CollisionTargetHandler(items)
    
    
    // NESTED   ----------------------------
    
    class CollisionTargetHandlerFactory(implicit log: Logger)
        extends FromCollectionFactory[CanCollideWith, CollisionTargetHandler]
    {
        override def from(items: IterableOnce[CanCollideWith]): CollisionTargetHandler = CollisionTargetHandler(items)
    }
}

/**
 * A handler used for managing and checking collisions against multiple
 * instances. The instances are grouped in specific subgroups, so that collision check-targeting may be more specific.
  *
  * These handlers are suited to be used by a single instance at a time, only.
  * Typically this instance is a CollisionHandler.
  *
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class CollisionTargetHandler(initialItems: IterableOnce[CanCollideWith])(implicit log: Logger)
    extends CollisionPartyHandler[CanCollideWith](initialItems)
{
    // IMPLEMENTED  ----------------------
    
    // Provides public access to the updated collision targets
    override def popUpdatedTargets() = super.popUpdatedTargets()
    
    override protected def asHandleable(item: Handleable): Option[CanCollideWith] = item match {
        case c: CanCollideWith => Some(c)
        case _ => None
    }
    
    
    // OTHER    --------------------------
    
    /**
     * Finds all collisions (in the specified collision groups) for the specified collision shape
     * @param shape the shape against which the collisions are checked
     * @param limitToGroups None if all of the instances should be checked. A collection
     * of checked collision groups otherwise
     * @return All collisions in the checked groups along with their participants
     */
    def checkForCollisions(shape: CollisionShape, limitToGroups: Option[Iterable[CollisionGroup]] = None) =
    {
        items.flatMap { c =>
            if (limitToGroups.forall { _.exists(c.collisionGroups.contains) })
                shape.checkCollisionWith(c.collisionShape).map { c -> _ }
            else
                None
        }
    }
}