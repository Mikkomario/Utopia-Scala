package utopia.flow.async

import utopia.flow.datastructure.mutable.Settable
import utopia.flow.event.{ChangeDependency, ChangeListener, Changing}

object Volatile
{
    /**
     * Creates a new volatile value
     */
    def apply[A](value: A) = new Volatile(value)
}

/**
* This class wraps a value that may be changed from multiple threads. The class itself is 
* mutable, but should only be used with types that have value semantics.
* @author Mikko Hilpinen
* @since 27.3.2019
**/
class Volatile[A](@volatile private var _value: A) extends Changing[A] with Settable[A]
{
    // ATTRIBUTES   ----------------
    
    override var listeners = Vector[ChangeListener[A]]()
    override var dependencies = Vector[ChangeDependency[A]]()
    
    /**
      * An immutable view of this volatile instance
      */
    lazy val valueView: Changing[A] = new View()
    
    
    // COMPUTED    -----------------
    
    /**
     * The current value of this volatile container
     */
    @deprecated("Please use .value instead", "v1.9")
    def get = value
    
    
    // IMPLEMENTED  ----------------
    
    override def value = this.synchronized { _value }
    
    override def value_=(newValue: A) = this.synchronized { setValue(newValue) }
    
    override def isChanging = true
    
    
    // OTHER    --------------------
    
    /**
     * Sets a new value to this container
     */
    @deprecated("Please assign directly to .value instead", "v1.9")
    def set(newValue: A) = value = newValue
    
    /**
      * Sets a new value to this container, but only if the specified condition is met
      * @param condition Condition checked on the value
      * @param newValue New value set for this volatile, if the condition is met. The value is call by name, so it's
      *                 only evaluated if the condition is met.
      */
    def setIf(condition: A => Boolean)(newValue: => A) = this.synchronized
    {
        if (condition(_value))
            setValue(newValue)
    }
    
    /**
     * Safely updates the value in this container
     */
    override def update(mutate: A => A) = this.synchronized { setValue(mutate(_value)) }
    
    /**
     * Safely updates the value in this container, then returns it
     */
    def updateAndGet(mutate: A => A) = this.synchronized {
        setValue(mutate(_value))
        _value
    }
    
    /**
     * Updates this volatile only if specified condition is met
     * @param condition A condition for updating
     * @param mutate A mutating function
     */
    def updateIf(condition: A => Boolean)(mutate: A => A) = this.synchronized
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
    def updateIfAndGet(condition: A => Boolean)(mutate: A => A) = this.synchronized
    {
        if (condition(_value))
            setValue(mutate(_value))
        _value
    }
    
    /**
     * Updates a value in this container. Returns the state before the update.
     */
    def takeAndUpdate[B](taker: A => B)(updater: A => A) = this.synchronized
    {
        val result = taker(_value)
        setValue(updater(_value))
        result
    }
    
    /**
     * Updates a value in this container. Also returns a result value.
     */
    def pop[B](mutate: A => (B, A)) = this.synchronized
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
    def lock[U](operation: A => U) = this.synchronized { operation(_value) }
    
    /**
     * Reads the current value of this volatile container and then changes it
     * @param newValue the new value for this volatile container
     * @return the value before the assignment
     */
    def getAndSet(newValue: A) = pop { v => v -> newValue }
    
    /**
      * @param mutate An updating function for the current value of this volatile container
      * @return The value previous to the update
      */
    def getAndUpdate(mutate: A => A) = this.synchronized
    {
        val result = _value
        setValue(mutate(_value))
        result
    }
    
    // Call this only in a synchronized block
    private def setValue(newValue: A) =
    {
        val oldValue = _value
        _value = newValue
        fireChangeEvent(oldValue)
    }
    
    
    // NESTED   ---------------------------
    
    private class View extends Changing[A]
    {
        override def listeners = Volatile.this.listeners
    
        override def listeners_=(newListeners: Vector[ChangeListener[A]]) = Volatile.this.listeners = newListeners
        
        override def dependencies = Volatile.this.dependencies
    
        override def dependencies_=(newDependencies: Vector[ChangeDependency[A]]) =
            Volatile.this.dependencies = newDependencies
    
        override def isChanging = Volatile.this.isChanging
    
        override def value = Volatile.this.value
    }
}