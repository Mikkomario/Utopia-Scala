package utopia.flow.async

import utopia.flow.datastructure.immutable.Lazy

object VolatileOption
{
    /**
     * Creates a new filled volatile option
     */
    def apply[A](item: A) = new VolatileOption(Some(item))
    
    /**
     * Creates a new empty volatile option
     */
    def apply[A]() = new VolatileOption[A](None)
}

/**
* This is a mutable thread safe container that contains 0 or 1 value(s), like a mutable option
* @author Mikko Hilpinen
* @since 29.3.2019
**/
class VolatileOption[A](initialValue: Option[A]) extends Volatile[Option[A]](initialValue) with Iterable[A]
{
	// IMPLEMENTED    ---------------
    
    override def iterator: Iterator[A] = new OptionIterator
    
    override def foreach[U](f: A => U) = value.foreach(f)
    
    
    // COMPUTED ---------------------
    
    /**
     * @return Whether this option is not empty
     */
    def isDefined = value.isDefined
    
    
    // OTHER    ---------------------
    
    /**
     * Sets the item in this option
     * @param newValue the item item to be set
     */
    def setOne(newValue: A) = value = Some(newValue)
    
    /**
     * Clears any items from this option
     */
    def clear() = value = None
    
    /**
     * Removes and returns the item in this option, if there is one
     */
    def pop() = getAndSet(None)
    
    /**
     * Sets a new value this option, but only if there is no current value
      * @param newValue New value for this option (call by name)
     */
    def setIfEmpty(newValue: => Option[A]) = updateIf { _.isEmpty } { _ => newValue }
    
    /**
     * Sets a new value to this option (only if empty), then returns the resulting value
     * @param newValue A new value for this option (call by name)
     * @return This option's value after operation
     */
    def setIfEmptyAndGet(newValue: => Option[A]) = updateIfAndGet { _.isEmpty } { _ => newValue }
    
    /**
     * Sets a new value this option, but only if there is no current value
     */
    def setOneIfEmpty(newValue: => A) = setIfEmpty(Some(newValue))
    
    /**
     * Sets a new value to this option, then returns that value
     */
    def setOneAndGet(newValue: A) = pop { _ => newValue -> Some(newValue) }
    
    /**
     * Sets a new value to this option (only if empty), then returns the resulting value of this option
     * @param newValue A new value for this option (call by name)
     * @return This option's value after operation
     */
    def setOneIfEmptyAndGet(newValue: => A) = updateIfAndGet { _.isEmpty } { _ => Some(newValue) }.get
    
    
    // NESTED   ---------------------
    
    private class OptionIterator extends Iterator[A]
    {
        // ATTRIBUTES   -------------
        
        private val cachedNext = Lazy { value }
        private var isConsumed = false
        
        override def hasNext = !isConsumed && cachedNext.value.isDefined
        
        override def next() =
        {
            if (isConsumed)
                throw new NoSuchElementException("Called next() twice for OptionIterator")
            else
            {
                isConsumed = true
                cachedNext.value.get
            }
        }
    }
}