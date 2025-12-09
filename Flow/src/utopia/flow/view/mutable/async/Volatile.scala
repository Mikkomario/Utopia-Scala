package utopia.flow.view.mutable.async

import utopia.flow.collection.immutable.Empty
import utopia.flow.operator.Identity
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.{Pointer, PointerFactory}
import utopia.flow.view.template.SynchronizedView

object Volatile extends PointerFactory[Volatile]
{
    // COMPUTED -------------------
    
    /**
      * @return Factory for volatile containers that generate change events
      */
    def eventful = EventfulVolatile
    /**
     * @return Factory for lockable (eventful) volatile containers
     */
    def lockable = LockableVolatile
    
    /**
      * @return A new volatile switch
      */
    def switch = VolatileSwitch()
    
    /**
      * @param log Implicit logging implementation used for handling errors thrown by attached event listeners
      * @return Factory for volatile flags
      */
    def flag(implicit log: Logger) = VolatileFlag()
    
    
    // IMPLEMENTED    --------------
    
    /**
      * @param initialValue Value to assign to this container, initially
      * @tparam A Type of values in this container
      * @return A new volatile container
      */
    override def apply[A](initialValue: A): Volatile[A] = new _Volatile[A](initialValue)
    
    
    // NESTED   -------------------
    
    private class _Volatile[A](initialValue: A) extends Volatile[A]
    {
        // ATTRIBUTES   -----------
        
        @volatile private var _value: A = initialValue
        
        
        // IMPLEMENTED  ----------
        
        override def value: A = _value
        
        override protected def assign(oldValue: A, newValue: A): Seq[() => Unit] = {
            _value = newValue
            Empty
        }
    }
}

/**
* Common trait for volatile containers, meaning containers which ensure safe access and mutation of a value
  * in a multithreaded environment.
  * @author Mikko Hilpinen
* @since 27.3.2019
**/
trait Volatile[A] extends Pointer[A] with SynchronizedView[A]
{
    // ABSTRACT --------------------
    
    /**
      * Changes the current value of this container.
      * This method will only be called from a synchronized block (i.e. this.synchronized { ... })
      * @param oldValue The old/current value of this item
     * @param newValue New value to assign to this container
      * @return Returns the after-effects to fire once outside the synchronized block
      */
    protected def assign(oldValue: A, newValue: A): Seq[() => Unit]
    
    
    // COMPUTED    -----------------
    
    /**
      * @return The current value of this volatile container, accessed in a synchronized manner,
      *         meaning that this function call will block while the value is being locked from another thread,
      *         during an update or such.
      *
      *         For non-synchronized access, which is perhaps faster but might be less accurate, call [[value]]
      */
    @deprecated("Deprecated for removal. If you need synchronized access to this pointer's value, use .lockWhile(...) instead", "v2.8")
    def synchronizedValue = lockWhile(Identity)
    
    
    // IMPLEMENTED  ----------------
    
    override def value_=(newValue: A) = mutate { _ => () -> newValue }
	
	override def viewLocked[B](operation: A => B) = this.synchronized { operation(value) }
	override def lockWhile[B](operation: => B): B = this.synchronized(operation)
	
	override def update(f: A => A): Unit = mutate { v => () -> f(v) }
    override def mutate[B](mutate: A => (B, A)) = {
        // Locks during operation & value change
        val (result, effects) = viewLocked { value =>
            // Performs the operation, acquires new value and final result
            val (result, newValue) = mutate(value)
            // Updates the wrapped value
            val afterEffects = assign(value, newValue)
            
            result -> afterEffects
        }
        // Performs the after-effects, if queued
        effects.foreach { _() }
        result
    }
    
    override def updateAndGet(f: A => A) = mutate { old =>
        val newVal = f(old)
        newVal -> newVal
    }
    override def getAndUpdate(f: A => A) = mutate { old => old -> f(old) }
    override def getAndSet(newValue: A) = mutate { _ -> newValue }
    
    
    // OTHER    --------------------
    
    /**
      * Updates a value in this container. Returns the state before the update.
      */
    @deprecated("Deprecated for removal. Please use .getAndUpdate instead", "v2.5")
    def takeAndUpdate[B](taker: A => B)(updater: A => A) = mutate { v => taker(v) -> updater(v) }
}