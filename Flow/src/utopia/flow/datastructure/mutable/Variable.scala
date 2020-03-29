package utopia.flow.datastructure.mutable

import utopia.flow.datastructure.template.Property
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.DataType

object Variable
{
    /**
      * Creates a new empty variable
      * @param name The name of this variable
      * @param dataType The data type of this variable
      * @return A new empty variable
      */
    def empty(name: String, dataType: DataType) = new Variable(name, Value.emptyWithType(dataType))
}

/**
 * A variable is a property whose value can be changed
 * @author Mikko Hilpinen
 * @since 27.11.2016
 * @param name The name of the variable. Immutable
 * @param content The initial content of the variable. This determines the data type of the
 * variable
 */
class Variable(val name: String, content: Value) extends PointerWithEvents[Value](content) with Property
{
    // ATTRIBUTES    -----------------
    
    // Makes sure the content is in correct data type
    override def value_=(value: Value) = super.value_=(value.withType(dataType))
    
    
    // COMP. PROPERTIES    -----------
    
    override def dataType = content.dataType
    
    /**
     * This variable as a constant copy
     */
    def toConstant = Constant(name, content)
}