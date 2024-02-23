package utopia.conflict.handling

import utopia.conflict.collision.CollisionShape
import utopia.flow.event.listener.ChangeListener
import utopia.flow.event.model.ChangeResponse.Continue
import utopia.flow.event.model.{ChangeEvent, ChangeResponse}
import utopia.genesis.handling.template.{AbstractHandler, Handleable}

import scala.collection.mutable


/**
 * An abstract handler that tracks collision shape changes of the attached items.
  * This is useful for situations where you want to test for collisions between the updated items.
 * @author Mikko Hilpinen
 * @since 4.8.2017
 */
abstract class CollisionPartyHandler[A <: Handleable with HasCollisionShape](initialItems: IterableOnce[A])
    extends AbstractHandler[A](initialItems)
{
    // ATTRIBUTES   ----------------------
    
    // Contains the latest updated collision targets
    private val queuedTargets = mutable.Map[A, CollisionShape]()
    private val activeListeners = mutable.Map[A, CollisionUpdater]()
    
    
    // INITIAL CODE ----------------------
    
    // Whenever items get added or removed, attaches or detaches collision shape -listening
    itemsPointer.readOnly.addListener { e =>
        val (changes, _) = e.values.separateMatching
        // Detaches collision shape -listening from all items that are no longer handled
        changes.first.foreach { a => activeListeners.get(a).foreach(a.collisionShapePointer.removeListener) }
        // Starts listening on collision shape updates on the added items
        changes.second.foreach { a =>
            val listener = new CollisionUpdater(a)
            a.collisionShapePointer.addListenerAndSimulateEvent(CollisionShape.empty)(listener)
            activeListeners += (a -> listener)
        }
    }
    
    
    // OTHER    --------------------------
    
    /**
      * @return All items that had their collision shape updated since the last call to this function.
      *         Resets the collision shape update -tracking.
      */
    protected def popUpdatedTargets() = {
        val result = queuedTargets.toMap
        queuedTargets.clear()
        result
    }
    
    
    // NESTED   ------------------------
    
    private class CollisionUpdater(target: A) extends ChangeListener[CollisionShape]
    {
        // Records every time a collision item's collision area gets updated
        override def onChangeEvent(event: ChangeEvent[CollisionShape]): ChangeResponse = {
            queuedTargets += (target -> event.newValue)
            Continue
        }
    }
}