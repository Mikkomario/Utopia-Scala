package utopia.flow.async

import utopia.flow.event.Changing

object Volatile
{
    /**
     * Creates a new volatile value
     */
    def apply[T](value: T) = new Volatile(value)
}

/**
* This class wraps a value that may be changed from multiple threads. The class itself is 
* mutable, but should only be used with types that have value semantics.
* @author Mikko Hilpinen
* @since 27.3.2019
**/
class Volatile[T](@volatile private var _value: T) extends Changing[T]
{
    // COMPUTED    -----------------
    
    /**
     * The current value of this volatile container
     */
    def get = this.synchronized { _value }
    
    override def value = get
    
    
    // OTHER    --------------------
    
    /**
     * Sets a new value to this container
     */
    def set(newValue: T) = this.synchronized { setValue(newValue) }
    
    /**
      * Sets a new value to this container, but only if the specified condition is met
      * @param condition Condition checked on the value
      * @param newValue New value set for this volatile, if the condition is met. The value is call by name, so it's
      *                 only evaluated if the condition is met.
      */
    def setIf(condition: T => Boolean)(newValue: => T) = this.synchronized
    {
        if (condition(_value))
            setValue(newValue)
    }
    
    /**
     * Safely updates the value in this container
     */
    def update(mutate: T => T) = this.synchronized { setValue(mutate(_value)) }
    
    /**
     * Safely updates the value in this container, then returns it
     */
    def updateAndGet(mutate: T => T) = this.synchronized {
        setValue(mutate(_value))
        _value
    }
    
    /**
     * Updates this volatile only if specified condition is met
     * @param condition A condition for updating
     * @param mutate A mutating function
     */
    def updateIf(condition: T => Boolean)(mutate: T => T) = this.synchronized
    {
        if (condition(_value))
            setValue(mutate(_value))
    }
    
    /**
     * Updates this volatile only if specified condition is met
     * @param condition A condition for updating
     * @param mutate A mutating function
     * @return Value of this volatile after operation
     */
    def updateIfAndGet(condition: T => Boolean)(mutate: T => T) = this.synchronized
    {
        if (condition(_value))
            setValue(mutate(_value))
        _value
    }
    
    /**
     * Updates a value in this container. Returns the state before the update.
     */
    def takeAndUpdate[B](taker: T => B)(updater: T => T) = this.synchronized
    {
        val result = taker(_value)
        setValue(updater(_value))
        result
    }
    
    /**
     * Updates a value in this container. Also returns a result value.
     */
    def pop[B](mutate: T => (B, T)) = this.synchronized
    {
        val (result, next) = mutate(_value)
        setValue(next)
        result
    }
    
    /**
     * Locks the value in this container from outside sources during the operation. Use with caution.
      * @tparam U The result type of the operation
      * @return the result of the operation
     */
    def lock[U](operation: T => U) = this.synchronized { operation(_value) }
    
    /**
     * Reads the current value of this volatile container and then changes it
     * @param newValue the new value for this volatile container
     * @return the value before the assignment
     */
    def getAndSet(newValue: T) = pop { v => v -> newValue }
    
    // Call this only in a synchronized block
    private def setValue(newValue: T) =
    {
        val oldValue = _value
        _value = newValue
        fireChangeEvent(oldValue)
    }
}