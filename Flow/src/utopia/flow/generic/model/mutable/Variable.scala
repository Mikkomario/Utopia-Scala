package utopia.flow.generic.model.mutable

import utopia.flow.event.listener.ChangingStoppedListener
import utopia.flow.event.model.Destiny
import utopia.flow.event.model.Destiny.ForeverFlux
import utopia.flow.generic.model.immutable.{Constant, Value}
import utopia.flow.generic.model.template.Property
import utopia.flow.util.logging.Logger
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.{AbstractChanging, Changing, ChangingWrapper}

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
      * @param log Logging implementation for handling failures in change-event -handling
      * @return A new variable
      */
    def apply(name: String, initialValue: Value = Value.empty, isFixedType: Boolean = false,
              requireCastingSuccess: Boolean = false)
             (implicit log: Logger): Variable =
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
      * @param log Logging implementation for handling failures in change-event -handling
      * @return A new variable
      */
    def withFixedType(name: String, initialValue: Value = Value.empty, requireCastingSuccess: Boolean = false)
                     (implicit log: Logger) =
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
      * @param log Logging implementation for handling failures in change-event -handling
      * @return A new empty variable
      */
    def emptyWithFixedType(name: String, dataType: DataType, requireCastingSuccess: Boolean = false)
                          (implicit log: Logger) =
        withFixedType(name, Value.emptyWithType(dataType), requireCastingSuccess)
    
    
    // NESTED   -----------------------
    
    private abstract class AbstractVariable(override val name: String, initialValue: Value = Value.empty)
                                           (implicit log: Logger)
        extends AbstractChanging[Value] with Variable
    {
        // ATTRIBUTES   ---------------------
        
        private var _value = initialValue
        
        override lazy val readOnly: Changing[Value] = ChangingWrapper(this)
        
        
        // ABSTRACT -------------------------
        
        protected def processValue(value: Value): Value
        
        
        // IMPLEMENTED  ---------------------
        
        override def destiny: Destiny = ForeverFlux
        
        override def value: Value = _value
        override def value_=(newValue: Value): Unit = {
            val oldValue = _value
            // May process the value before assigning it
            _value = processValue(value)
            fireEventIfNecessary(oldValue).foreach { _() }
        }
        
        override protected def _addChangingStoppedListener(listener: => ChangingStoppedListener): Unit = ()
    }
    
    private class _Variable(override val name: String, initialValue: Value = Value.empty)(implicit log: Logger)
        extends AbstractVariable(name, initialValue)
    {
        override protected def processValue(value: Value): Value = value
    }
    
    private class FixedTypeVariable(override val name: String, override val dataType: DataType,
                                    initialValue: Value = Value.empty, requireCastingSuccess: Boolean = false)
                                   (implicit log: Logger)
        extends AbstractVariable(name, initialValue)
    {
        // INITIAL CODE ------------------
        
        value = cast(value)
        
        
        // IMPLEMENTED  ------------------
        
        override protected def processValue(value: Value): Value = cast(value)
        
        
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
trait Variable extends Property with EventfulPointer[Value]
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