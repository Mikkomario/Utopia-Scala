package utopia.flow.async

import utopia.flow.datastructure.mutable.Lazy

object VolatileOption
{
    /**
     * Creates a new filled volatile option
     */
    def apply[T](item: T) = new VolatileOption(Some(item))
    
    /**
     * Creates a new empty volatile option
     */
    def apply[T]() = new VolatileOption[T](None)
}

/**
* This is a mutable thread safe container that contains 0 or 1 value(s), like a mutable option
* @author Mikko Hilpinen
* @since 29.3.2019
**/
class VolatileOption[T](value: Option[T]) extends Volatile[Option[T]](value) with Iterable[T]
{
	// IMPLEMENTED    ---------------
    
    override def iterator: Iterator[T] = new OptionIterator
    
    override def foreach[U](f: T => U) = get.foreach(f)
    
    
    // OTHER    ---------------------
    
    /**
     * Sets the item in this option
     * @param newValue the item item to be set
     */
    def setOne(newValue: T) = set(Some(newValue))
    
    /**
     * Clears any items from this option
     */
    def clear() = set(None)
    
    /**
     * Removes and returns the item in this option, if there is one
     */
    def pop() = getAndSet(None)
    
    /**
     * Sets a new value this option, but only if there is no current value
     */
    def setIfEmpty(getValue: () => Option[T]) = updateIf { _.isEmpty }{ _ => getValue() }
    
    /**
     * Sets a new value to this option (only if empty), then returns the resulting value
     * @param getValue A function for generating a new value for this option
     * @return This option's value after operation
     */
    def setIfEmptyAndGet(getValue: () => Option[T]) = updateIfAndGet { _.isEmpty } { _ => getValue() }
    
    /**
     * Sets a new value this option, but only if there is no current value
     */
    def setOneIfEmpty(getValue: () => T) = setIfEmpty(() => Some(getValue()))
    
    /**
     * Sets a new value to this option, then returns that value
     */
    def setOneAndGet(newValue: T) = pop { _ => newValue -> Some(newValue) }
    
    /**
     * Sets a new value to this option (only if empty), then returns the resulting value of this option
     * @param getValue A function for generating a new value for this option
     * @return This option's value after operation
     */
    def setOneIfEmptyAndGet(getValue: () => T) = updateIfAndGet { _.isEmpty } { _ => Some(getValue()) }.get
    
    
    // NESTED   ---------------------
    
    private class OptionIterator extends Iterator[T]
    {
        // ATTRIBUTES   -------------
        
        private val cachedNext = Lazy { get }
        private var isConsumed = false
        
        override def hasNext = !isConsumed && cachedNext.get.isDefined
        
        override def next() =
        {
            if (isConsumed)
                throw new NoSuchElementException("Called next() twice for OptionIterator")
            else
            {
                isConsumed = true
                cachedNext.get.get
            }
        }
    }
}