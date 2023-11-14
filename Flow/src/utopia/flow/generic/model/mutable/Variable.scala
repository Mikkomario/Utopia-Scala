package utopia.flow.generic.model.mutable

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.ChangeEvent
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.template.Property
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing}

object Variable
{
    /**
      * Creates a new variable
      * @param name Name of this variable
      * @param initialValue Initial value for this variable (default = empty)
      * @param isFixedType Whether all values in this variable should be cast to its initial data type.
      *                    If false (default), the proposed values are assigned as is, resulting in possibly
      *                    varying data types (kind of duck typing).
      * @param requireCastingSuccess Whether, when casting values,
      *                              the casting is required to succeed in order for a proposed value to be assigned.
      *                              If true, an empty value will be assigned on casting failures.
      *                              If false (default), the proposed value will be assigned as is on casting failures.
      *                              If this variable doesn't have a fixed type (i.e. 'isFixedType' is false),
      *                              this parameter is irrelevant.
      * @return A new variable
      */
    def apply(name: String, initialValue: Value = Value.empty, isFixedType: Boolean = false,
              requireCastingSuccess: Boolean = false): Variable =
    {
        if (isFixedType)
            new FixedTypeVariable(name, initialValue.dataType, initialValue, requireCastingSuccess)
        else
            new _Variable(name, initialValue)
    }
    
    /**
      * Creates a new variable where values are cast to a specific data type before they are assigned
      * @param name Name of this variable
      * @param initialValue The initially assigned value (default = empty)
      * @param requireCastingSuccess Whether, when casting values,
      *                              the casting is required to succeed in order for a proposed value to be assigned.
      *                              If true, an empty value will be assigned on casting failures.
      *                              If false (default), the proposed value will be assigned as is on casting failures.
      * @return A new variable
      */
    def withFixedType(name: String, initialValue: Value = Value.empty, requireCastingSuccess: Boolean = false) =
        apply(name, initialValue, isFixedType = true, requireCastingSuccess)
    /**
      * Creates a new variable where values are cast to a specific data type before they are assigned.
      * This variable is empty initially.
      * @param name Name of this variable
      * @param dataType The data type to which all values will be cast before they are assigned
      * @param requireCastingSuccess Whether, when casting values,
      *                              the casting is required to succeed in order for a proposed value to be assigned.
      *                              If true, an empty value will be assigned on casting failures.
      *                              If false (default), the proposed value will be assigned as is on casting failures.
      * @return A new empty variable
      */
    def emptyWithFixedType(name: String, dataType: DataType, requireCastingSuccess: Boolean = false) =
        withFixedType(name, Value.emptyWithType(dataType), requireCastingSuccess)
    
    
    // NESTED   -----------------------
    
    private class _Variable(override val name: String, initialValue: Value = Value.empty)
        extends EventfulPointer[Value](initialValue) with Variable
    
    private class FixedTypeVariable(override val name: String, override val dataType: DataType,
                                    initialValue: Value = Value.empty, requireCastingSuccess: Boolean = false)
        extends AbstractChanging[Value] with Variable
    {
        // ATTRIBUTES   ------------------
        
        private var _value = cast(initialValue)
        
        
        // IMPLEMENTED  ------------------
        
        override def value = _value
        override def value_=(value: Value) = {
            // Casts to correct data type before assigning
            val castValue = cast(value)
            if (value != _value) {
                val event = ChangeEvent(_value, castValue)
                _value = castValue
                fireEvent(event).foreach { _() }
            }
        }
    
        override def isChanging = true
        override def mayStopChanging: Boolean = false
        
        override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
        
        
        // OTHER    ---------------------
        
        private def cast(value: Value) =
            value.castTo(dataType).getOrElse { if (requireCastingSuccess) Value.emptyWithType(dataType) else value }
    }
}

/**
 * A variable is a property that allows value mutations
 * @author Mikko Hilpinen
 * @since 27.11.2016
 */
trait Variable extends Property with Pointer[Value] with Changing[Value]
{
    // COMP. PROPERTIES    -----------
    
    /**
      * @return The (current) data type of this variable
      */
    def dataType: DataType = value.dataType
    
    /**
     * A constant based on this variable's current state
     */
    def toConstant = Constant(name, value)
}