package utopia.flow.collection.mutable.iterator

/**
* A counter runs an increasing number and will not return the same number twice (unless incremented 
* over integer max value)
* @author Mikko Hilpinen
* @since 12.5.2018
**/
class Counter(private val firstValue: Int, private val increment: Int = 1) extends Generator[Int]
{
    // ATTRIBUTES    ------------------
    
    private var nextNumber = firstValue
    
    
    // IMPLEMENTED METHODS    ---------
    
    def next() = 
    {
        val result = nextNumber
        if (nextNumber > Int.MaxValue - increment)
            nextNumber = firstValue
        else
            nextNumber += 1
            
        result
    }
}