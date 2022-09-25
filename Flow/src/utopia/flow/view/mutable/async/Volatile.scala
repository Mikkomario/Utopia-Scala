package utopia.flow.view.mutable.async

import utopia.flow.event.model.ChangeEvent
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.template.eventful.{AbstractChanging, ChangingWrapper}

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
class Volatile[A](@volatile private var _value: A) extends AbstractChanging[A] with Pointer[A]
{
    // ATTRIBUTES   ----------------
    
    /**
      * An immutable view of this volatile instance
      */
    lazy val valueView = ChangingWrapper(this)
    
    
    // COMPUTED    -----------------
    
    /**
     * The current value of this volatile container
     */
    @deprecated("Please use .value instead", "v1.9")
    def get = value
    
    /**
      * @return The current value of this volatile container, accessed in a synchronized manner,
      *         meaning that this function call will block while the value is being locked from another thread,
      *         during an update or such. For non-synchronized access, which is perhaps faster but might be less
      *         accurate, call `.value`
      * @see value
      */
    def synchronizedValue = this.synchronized { _value }
    
    
    // IMPLEMENTED  ----------------
    
    override def value = _value
    override def value_=(newValue: A) = lockAndSet { _ => () -> newValue }
    
    override def isChanging = true
    
    /**
      * Safely updates the value in this container
      */
    override def update(mutate: A => A) = lockAndSet { v => () -> mutate(v) }
    /**
      * Safely updates the value in this container, then returns it
      */
    override def updateAndGet(mutate: A => A) = lockSetAndGet(mutate)
    
    /**
      * Updates a value in this container. Also returns a result value.
      */
    def pop[B](mutate: A => (B, A)) = lockAndSet(mutate)
    
    /**
      * Reads the current value of this volatile container and then changes it
      * @param newValue the new value for this volatile container
      * @return the value before the assignment
      */
    override def getAndSet(newValue: A) = lockAndSet { v => v -> newValue }
    /**
      * @param mutate An updating function for the current value of this volatile container
      * @return The value previous to the update
      */
    override def getAndUpdate(mutate: A => A) = lockAndSet { v => v -> mutate(v) }
    
    
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
      * @return The value of this volatile container after the update
      */
    def setIf(condition: A => Boolean)(newValue: => A) = updateIf(condition) { _ => newValue }
    
    /**
     * Updates this volatile only if specified condition is met
     * @param condition A condition for updating
     * @param mutate A mutating function (only called if condition applies)
      * @return Value of this volatile container after the update
     */
    def updateIf(condition: A => Boolean)(mutate: A => A) = lockSetAndGet { v => if (condition(v)) mutate(v) else v }
    /**
     * Updates this volatile only if specified condition is met
     * @param condition A condition for updating
     * @param mutate A mutating function (only called if condition applies)
     * @return Value of this volatile after operation
     */
    @deprecated("Replaced with updateIf, that now performs the exact same operation", "v1.16")
    def updateIfAndGet(condition: A => Boolean)(mutate: A => A) = updateIf(condition)(mutate)
    
    /**
     * Updates a value in this container. Returns the state before the update.
     */
    def takeAndUpdate[B](taker: A => B)(updater: A => A) = lockAndSet { v => taker(v) -> updater(v) }
    
    /**
     * Locks the value in this container from outside sources during the operation. Use with caution.
      * @tparam U The result type of the operation
      * @return the result of the operation
     */
    def lock[U](operation: A => U) = this.synchronized { operation(_value) }
    
    private def lockSetAndGet(operation: A => A) = lockAndSet { v =>
        val newVal = operation(v)
        newVal -> newVal
    }
    
    private def lockAndSet[U](operation: A => (U, A)) = {
        // Locks during operation & value change
        val (result, changeEvent) = this.synchronized {
            // Performs the operation, acquires new value and final result
            val (result, newValue) = operation(_value)
            // Updates the value (if necessary), acquires a change event to fire
            val changeEvent = {
                if (newValue == _value)
                    None
                else {
                    val oldValue = _value
                    _value = newValue
                    Some(ChangeEvent(oldValue, newValue))
                }
            }
            result -> changeEvent
        }
        // Fires the change event, if necessary
        changeEvent.foreach(fireEvent)
        // Returns the custom result
        result
    }
}