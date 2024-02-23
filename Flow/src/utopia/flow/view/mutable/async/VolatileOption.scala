package utopia.flow.view.mutable.async

import utopia.flow.view.immutable.caching.Lazy

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
class VolatileOption[A](initialValue: Option[A])
    extends Volatile[Option[A]](initialValue) with Iterable[A]
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
      * @return value after the update (i.e. 'newValue')
     */
    def setOne(newValue: A) = mutate { _ => newValue -> Some(newValue) }
    
    /**
     * Clears any items from this option
      * @return Whether the state of this option changed
     */
    def clear() = mutate { _.isDefined -> None }
    
    /**
     * Removes and returns the item in this option, if there is one
     */
    def pop() = getAndSet(None)
    
    /**
     * Sets a new value this option, but only if there is no current value
      * @param newValue New value for this option (call by name)
      * @return Value after update
     */
    def setIfEmpty(newValue: => Option[A]) = setIf { _.isEmpty } (newValue)
    
    /**
     * Sets a new value this option, but only if there is no current value
      * @return Value after the update
     */
    def setOneIfEmpty(newValue: => A) = mutate {
        case Some(v) => v -> Some(v)
        case None =>
            val newVal = newValue
            newVal -> Some(newVal)
    }
    
    
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