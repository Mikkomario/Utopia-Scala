package utopia.flow.datastructure.immutable

import utopia.flow.datastructure.template.Property
import utopia.flow.datastructure.mutable.Variable

/**
 * Constants are named properties whose value can't be changed
 * @author Mikko Hilpinen
 * @since 29.11.2016
 */
case class Constant(name: String, value: Value) extends Property
{
    // COMP. PROPERTIES    ---------
    
    override val dataType = value.dataType
    
    
    // COMPUTED    -----------------
    
    /**
     * Converts this constant to a variable
     */
    def toVariable = new Variable(name, value)
    
    
    // OTHER METHODS    ------------
    
    /**
     * Creates a new constant that has the provided value but the same name
     * @param value the value the new constant will have
     */
    def withValue(value: Value) = copy(value = value)
    
    /**
      * @param name New name for constant
      * @return A copy of this constant with provided name
      */
    def withName(name: String) = copy(name = name)
}