package utopia.flow.view.mutable.async

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.util.logging.Logger
import utopia.flow.view.template.eventful.ChangingWrapper

@deprecated("Deprecated for removal. Please use the new version instead", "v2.5")
object VolatileOld
{
    /**
     * Creates a new volatile container
      * @param log Implicit logging implementation for handling failures thrown by event listeners
      * @param value Initial value to wrap
     */
    def apply[A](value: A)(implicit log: Logger) = new VolatileOld(value)
}

/**
* This class wraps a value that may be changed from multiple threads.
* @param _value Initial value to place in this container
  * @author Mikko Hilpinen
* @since 27.3.2019
**/
@deprecated("Deprecated for removal. Please use the new version instead", "v2.5")
class VolatileOld[A](@volatile private var _value: A)(implicit log: Logger) extends EventfulVolatile[A]
{
    // ATTRIBUTES   ----------------
    
    // Caches the read-only view
    override lazy val readOnly = ChangingWrapper(this)
    
    
    // COMPUTED    -----------------
    
    /**
      * An immutable view of this volatile instance
      */
    @deprecated("Please switch to using .readOnly instead", "v2.3")
    def valueView = readOnly
    
    
    // IMPLEMENTED  ----------------
    
    override def value = _value
    override def destiny: Destiny = ForeverFlux
    
    override protected def assignWithoutEvents(newValue: A): Unit = _value = newValue
    
    override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
    
    
    // OTHER    --------------------
    
    /**
      * Updates a value in this container. Also returns a result value.
      */
    @deprecated("Renamed to .mutate(...)", "v2.3")
    def pop[B](mutate: A => (B, A)) = this.mutate(mutate)
}