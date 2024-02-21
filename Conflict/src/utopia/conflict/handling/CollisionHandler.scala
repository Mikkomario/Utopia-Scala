package utopia.conflict.handling

import utopia.conflict.collision.{Collision, CollisionShape}
import utopia.flow.collection.template.factory.FromCollectionFactory
import utopia.flow.view.template.eventful.FlagLike
import utopia.genesis.handling.action.Actor
import utopia.genesis.handling.template.Handleable2

import scala.concurrent.duration.FiniteDuration

@deprecated("Replaced with a new version", "v1.5")
object CollisionHandler
{
    // OTHER    -------------------------
    
    /**
      * @param targets A handler that manages collision targets
      * @return A factory for constructing a collision handler that checks for collisions with those targets
      */
    def against(targets: CollisionTargetHandler) = new CollisionHandlerFactory(targets)
    
    
    // NESTED   -------------------------
    
    class CollisionHandlerFactory(targetHandler: CollisionTargetHandler)
        extends FromCollectionFactory[CollisionListener, CollisionHandler]
    {
        // IMPLEMENTED  -----------------
        
        override def from(items: IterableOnce[CollisionListener]): CollisionHandler = apply(items)
        
        
        // OTHER    ---------------------
        
        def apply(listeners: IterableOnce[CollisionListener]) = new CollisionHandler(targetHandler, listeners)
    }
}

/**
 * A collision handler handles collision-checking between [[CollisionListener]]s and [[CanCollideWith]] instances.
  * A CollisionHandler needs to be added to a working ActorHandler in order for it to deliver collision events.
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
class CollisionHandler(targetHandler: CollisionTargetHandler,
                       initialListeners: IterableOnce[CollisionListener] = Iterable.empty)
    extends CollisionPartyHandler[CollisionListener](initialListeners) with Actor
{
    // ATTRIBUTES   --------------------
    
    override val handleCondition: FlagLike = itemsPointer.readOnly.map { _.nonEmpty }
    
    // Contains the collisions that are active at any time
    private var activeCollisions = Map[CollisionListener, Iterable[(CanCollideWith, Collision)]]()
    
    
    // IMPLEMENTED    ------------------
    
    override def act(duration: FiniteDuration) = {
        // Makes sure collisions are up-to-date
        updateCollisions()
        // Informs listeners of active collisions
        activeCollisions.foreach { case (listener, collisions) => listener.onCollision(collisions, duration) }
    }
    
    override protected def asHandleable(item: Handleable2): Option[CollisionListener] = item match {
        case l: CollisionListener => Some(l)
        case _ => None
    }
    
    
    // OTHER    -----------------------------
    
    private def updateCollisions() = {
        val updatedListeners = popUpdatedTargets()
        val updatedTargets = targetHandler.popUpdatedTargets()
        
        // Checks for collisions for the updated listeners
        val updatedListenerCollisions = updatedListeners.map { case (listener, shape) =>
            val collisions = targetHandler.checkForCollisions(shape, listener.targetCollisionGroups)
                .filterNot { _._1 == listener }
            listener -> collisions
        }
        
        // Also checks for collisions against any updated collision target
        val updatedTargetCollisions:  Map[CollisionListener, Iterable[(CanCollideWith, Collision)]] = {
            if (updatedTargets.nonEmpty) {
                items.iterator.filterNot(updatedListeners.contains).map { listener =>
                    // Finds the targets that belong to the targeted collision groups
                    val targetGroups = listener.targetCollisionGroups
                    val possibleTargets = targetGroups match {
                        case Some(targetGroups) =>
                            if (targetGroups.isEmpty)
                                Map[CanCollideWith, CollisionShape]()
                            else
                                updatedTargets
                                    .filter { case (target, _) => targetGroups.exists(target.collisionGroups.contains) }
                        case None => updatedTargets
                    }
                    if (possibleTargets.nonEmpty) {
                        // Tests for collisions with the updated targets
                        val shape = listener.collisionShape
                        val collisions = possibleTargets.flatMap { case (target, targetShape) =>
                            shape.checkCollisionWith(targetShape).map { target -> _ }
                        }
                        listener -> collisions
                    }
                    else
                        listener -> Map.empty
                }.toMap
            }
            else
                Map()
        }
        
        activeCollisions = (updatedListenerCollisions ++ updatedTargetCollisions).filterNot { _._2.isEmpty }
    }
}