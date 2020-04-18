package utopia.flow.util

object Generator
{
    /**
     * Creates a general generator using a function and a starting value
     */
    def apply[T](startValue: T)(increase: T => T): Generator[T] = new SimpleGenerator(startValue, increase)
	
	private class SimpleGenerator[T](startValue: T, val increase: T => T) extends Generator[T]
	{
		var nextValue = startValue
		
		def next() =
		{
			val result = nextValue
			nextValue = increase(result)
			result
		}
	}
}

/**
* Generators generate new values and are supposed to be able to generate unlimited amount of items
* @author Mikko Hilpinen
* @since 12.5.2018
**/
trait Generator[+T]
{
    // ABSTRACT    ------------------
    
    /**
     * Generates a new value
     */
	def next(): T
	
	
	// OTHER METHODS    -------------
	
	/**
	 *  An infinite iterator for this generator
	 */
	def iterator = Iterator.continually(next())
}