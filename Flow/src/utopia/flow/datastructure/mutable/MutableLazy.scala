package utopia.flow.datastructure.mutable

object MutableLazy
{
    /**
      * Creates a new lazy
      * @param make A function that makes the value (call by name)
      * @tparam A The type of cached item
      * @return A new lazy container
      */
    def apply[A](make: => A) = new MutableLazy(make)
}

/**
* This is a mutable version of a lazy variable, meaning that the value may be changed and reset.
* This lazy caches the results of a function on the first call and after calls following a reset.
* @author Mikko Hilpinen
* @since 26.2.2019
**/
class MutableLazy[A](generator: => A) extends MutableLazyLike[A]
{
	// ATTRIBUTES    ----------------
    
    private var _value: Option[A] = None
    
    
    // IMPLEMENTED    ---------------
    
    override def value_=(newValue: A) = _value = Some(newValue)
    
    override def reset() = _value = None
    
    override def value = current match
    {
        case Some(value) => value
        case None =>
            val newValue = generator
            _value = Some(newValue)
            newValue
    }
    
    override def current = _value
}