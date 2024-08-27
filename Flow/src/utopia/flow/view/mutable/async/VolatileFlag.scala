package utopia.flow.view.mutable.async

import utopia.flow.util.logging.Logger
import utopia.flow.view.immutable.eventful.FlagView
import utopia.flow.view.mutable.eventful.ResettableFlag
import utopia.flow.view.template.eventful.Flag

object VolatileFlag
{
    /**
      * Creates a new volatile flag
      * @param initialState Initial state of this flag
      * @return A new volatile flag
      */
    def apply(initialState: Boolean = false)(implicit log: Logger)  = new VolatileFlag(initialState)
}

/**
* A volatile flag is used for marking singular events (flags) in a multi-threaded environment
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class VolatileFlag(initialState: Boolean = false)(implicit log: Logger)
    extends Volatile[Boolean](initialState) with ResettableFlag
{
    // ATTRIBUTES   --------------
    
    override lazy val readOnly: Flag = new FlagView(this)
    
    
    // IMPLEMENTED  ---------------
    
    override def view = readOnly
    
    
	// OTHER    ------------------
    
    /**
     * Sets this flag and also returns the state before conversion
     */
    def getAndSet(): Boolean = getAndSet(newValue = true)
    /**
     * Resets this flag
     * @return Value before this flag was reset
     */
    def getAndReset() = getAndSet(newValue = false)
    
    /**
     * If this flag is not set, performs the operation. Locks this flag while the operation runs.
     */
    @deprecated("Use of this function is discouraged, as it is prone to dead-locks. Please consider using if (isNotSet) instead", "v1.17")
    def doIfNotSet[U](action: => U) = lock { status => if (!status) action }
    /**
     * If this flag is not set, performs the operation and then sets this flag. 
     * Locks this flag during the operation.
     */
    @deprecated("Use of this function is discouraged, as it is prone to dead-locks. Please consider using if (set()) instead", "v1.17")
    def runAndSet[U](action: => U) = updateIf { !_ } { _ =>
        action
        true
    }
    /**
      * Sets this flag and performs the provided function, but only if this flag is not yet set
      * @param f A function for producing a result
      * @tparam B Result type
      * @return Result if this flag was not set. None otherwise.
      */
    @deprecated("Use of this function is discouraged, as it is prone to dead-locks. Please consider using if (set()) instead", "v1.17")
    def mapAndSet[B](f: => B) = pop { status =>
        if (status)
            None -> status
        else
            Some(f) -> true
    }
}