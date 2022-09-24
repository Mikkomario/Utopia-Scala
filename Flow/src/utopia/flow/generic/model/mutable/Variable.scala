package utopia.flow.generic.model.mutable

import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.template.Property
import utopia.flow.view.mutable.eventful.PointerWithEvents

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
 * @param initialContent The initial content of the variable. This determines the data type of the
 * variable
 */
class Variable(override val name: String, initialContent: Value)
    extends PointerWithEvents[Value](initialContent) with Property
{
    // ATTRIBUTES   ------------------
    
    override val dataType = initialContent.dataType
    
    
    // COMP. PROPERTIES    -----------
    
    /**
     * This variable as a constant copy
     */
    def toConstant = Constant(name, value)
    
    
    // IMPLEMENTED  ------------------
    
    // Makes sure the content is in correct data type
    override def value_=(value: Value) = super.value_=(value.withType(dataType))
}