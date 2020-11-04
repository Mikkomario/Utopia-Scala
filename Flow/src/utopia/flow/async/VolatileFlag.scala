package utopia.flow.async

/**
* A volatile flag is used for marking singular events (flags) in a multi-threaded environment
* @author Mikko Hilpinen
* @since 28.3.2019
**/
class VolatileFlag(initialState: Boolean = false) extends Volatile[Boolean](initialState)
{
    // COMPUTED    ---------------
    
    /**
     * Whether this flag is currently set
     */
    def isSet = value
    
    /**
      * @return Whether this flag isn't currently set
      */
    def notSet = !isSet
    
    
	// OTHER    ------------------
    
    /**
     * Sets this flag (same as set(true))
     */
    def set(): Unit = value = true
    
    /**
     * Resets this flag (same as set(false))
     */
    def reset() = value = false
    
    /**
     * Sets this flag and also returns the state before conversion
     */
    def getAndSet(): Boolean = getAndSet(true)
    
    /**
     * If this flag is not set, performs the operation. Locks this flag while the operation runs.
     */
    def doIfNotSet[U](action: => U) = lock { status => if (!status) action }
    
    /**
     * If this flag is not set, performs the operation and then sets this flag. 
     * Locks this flag during the operation.
     */
    def runAndSet[U](action: => U) = update
    {
        status => 
            if (!status)
                action
            true
    }
    
    /**
      * Sets this flag and performs the provided function, but only if this flag is not yet set
      * @param f A function for producing a result
      * @tparam B Result type
      * @return Result if this flag was not set. None otherwise.
      */
    def mapAndSet[B](f: => B) = pop { status =>
        if (status)
            None -> status
        else
            Some(f) -> true
    }
}