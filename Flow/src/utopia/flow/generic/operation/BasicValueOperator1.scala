package utopia.flow.generic.operation

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.enumeration.ValueOperation1
import utopia.flow.generic.model.enumeration.ValueOperation1.Negate
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType._

/**
 * Provides unary operations to Flow-originated value types
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
object BasicValueOperator1 extends ValueOperator1
{
	// ATTRIBUTES   ------------------------
	
	override val supportedTypes: Iterable[(DataType, ValueOperation1)] =
		Vector(StringType, IntType, DoubleType, FloatType, LongType, BooleanType, DurationType, DaysType,
			YearType, PairType, VectorType)
			.map[(DataType, ValueOperation1)] { _ -> Negate }
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(value: Value, operation: ValueOperation1): Value = value.dataType match {
		case StringType => value.getString.reverse
		case IntType => -value.getInt
		case DoubleType => -value.getDouble
		case LongType => -value.getLong
		case FloatType => -value.getFloat
		case BooleanType => !value.getBoolean
		case DurationType => -value.getDuration
		case DaysType => -value.getDays
		case YearType => -value.getYear
		case PairType => value.getPair.reverse
		case VectorType => value.getVector.reverse
		case other => throw new UnsupportedOperationException(s"Cannot negate a value of type $other")
	}
}
