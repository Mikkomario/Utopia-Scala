package utopia.flow.generic.operation

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.enumeration.ValueOperation2
import utopia.flow.generic.model.enumeration.ValueOperation2.{CombineValuesOperation, Minus, Plus}
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType._
import utopia.flow.operator.sign.Sign
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.YearMonth

import java.time.LocalDate

/**
 * Provides support for operating Flow-originated value types
 * @author Mikko Hilpinen
 * @since 10.11.2026, v2.9
 */
object BasicValueOperator2 extends ValueOperator2
{
	// ATTRIBUTES   ------------------------
	
	override val supportedOperations: Iterable[(DataType, ValueOperation2, Set[DataType])] = Vector.concat(
		// Strings can only be concatenated
		supportEntries(StringType, plus = Set(StringType)),
		// Number types can be combined with number types
		supportEntries(IntType, combine = Set(IntType, DoubleType, LongType, FloatType)),
		supportEntries(DoubleType, combine = Set(DoubleType)),
		supportEntries(FloatType, combine = Set(FloatType, DoubleType)),
		supportEntries(LongType, combine = Set(LongType, DoubleType)),
		// Timestamps support addition and subtraction with durations,
		// as well as subtraction (diff) with other timestamps
		supportEntries(InstantType, combine = Set(DurationType), minus = Set(InstantType)),
		supportEntries(LocalDateTimeType, combine = Set(DurationType), minus = Set(LocalDateTimeType)),
		supportEntries(LocalDateType, combine = Set(DaysType, DurationType),
			plus = Set(LocalTimeType), minus = Set(LocalDateType, LocalDateTimeType)),
		supportEntries(LocalTimeType, combine = Set(DurationType),
			plus = Set(LocalDateType), minus = Set(LocalTimeType)),
		// Duration types can be combined and subtracted with each other.
		// They can also be added to timestamps.
		supportEntries(DurationType, combine = Set(DurationType),
			plus = Set(InstantType, LocalDateTimeType, LocalTimeType)),
		supportEntries(DaysType, combine = Set(DaysType, DurationType),
			plus = Set(LocalDateType, LocalDateTimeType, InstantType)),
		// Years can be converted to timestamps and year months.
		supportEntries(YearType, combine = Set(DaysType, DurationType), plus = Set(MonthType)),
		// Months can be converted to year months
		supportEntries(MonthType, plus = Set(YearType)),
		// Year months can be converted to timestamps by adding durations.
		// They can also be compared with timestamps and year months.
		supportEntries(YearMonthType, combine = Set(DaysType, DurationType), minus = Set(YearMonthType, LocalDateType)),
		// Collection types may be appended and filtered.
		supportEntries(PairType, plus = Set(VectorType), minus = Set(AnyType)),
		supportEntries(VectorType, plus = Set(VectorType), minus = Set(AnyType))
	)
	
	
	// IMPLEMENTED  ------------------------
	
	override def apply(primary: Value, operation: ValueOperation2, secondary: Value): Value = {
		lazy val combineOperation = operation match {
			case o: CombineValuesOperation => o
			case _ => throw new UnsupportedOperationException(s"Unsupported operation: $operation")
		}
		lazy val sign = combineOperation.sign
		
		primary.dataType match {
			case StringType => s"$primary$secondary"
			case IntType => intAnd(primary.getInt, secondary, sign)
			case DoubleType => primary.getDouble + (secondary.getDouble * sign.modifier)
			case FloatType =>
				secondary.dataType match {
					case DoubleType => primary.getFloat + (secondary.getDouble * sign.modifier)
					case _ => primary.getFloat + (secondary.getFloat * sign.modifier)
				}
			case LongType =>
				secondary.dataType match {
					case DoubleType => primary.getLong + (secondary.getDouble * sign.modifier)
					case _ => primary.getLong + (secondary.getLong * sign.modifier)
				}
			case InstantType =>
				secondary.dataType match {
					case InstantType => primary.getInstant - secondary.getInstant
					case _ => primary.getInstant + (secondary.getDuration * sign.modifier)
				}
			case LocalDateTimeType =>
				secondary.dataType match {
					case LocalDateTimeType => primary.getLocalDateTime - secondary.getLocalDateTime
					case _ => primary.getLocalDateTime + (secondary.getDuration * sign.modifier)
				}
			case LocalDateType => localDateAnd(primary.getLocalDate, secondary, sign)
			case LocalTimeType =>
				secondary.dataType match {
					case LocalTimeType => primary.getLocalTime - secondary.getLocalTime
					case LocalDateType => secondary.getLocalDate + primary.getLocalTime
					case _ => primary.getLocalTime + (secondary.getDuration * sign.modifier)
				}
			case DurationType =>
				val d = primary.getDuration
				secondary.dataType match {
					case InstantType => secondary.getInstant + d
					case LocalDateTimeType => secondary.getLocalDateTime + d
					case LocalTimeType => secondary.getLocalTime + d
					case _ => d + (secondary.getDuration * sign.modifier)
				}
			case DaysType =>
				val d = primary.getDays
				secondary.dataType match {
					case LocalDateTimeType => secondary.getLocalDateTime + d
					case LocalDateType => secondary.getLocalDate + d
					case InstantType => secondary.getInstant + d
					case DaysType => d + (secondary.getDays * sign.modifier)
					case _ => d + (secondary.getDuration * sign.modifier)
				}
			case YearType =>
				val y = primary.getYear
				secondary.dataType match {
					case DaysType => y.firstDay + (secondary.getDays * sign.modifier)
					case DurationType => y.firstDay.atStartOfDay + (secondary.getDuration * sign.modifier)
					case _ => y + secondary.getMonth
				}
			case MonthType => secondary.getYear + primary.getMonth
			case YearMonthType => yearMonthAnd(primary.getYearMonth, secondary, sign)
			case PairType => collectionAnd(primary.getPair, secondary, combineOperation)
			case VectorType => collectionAnd(primary.getVector, secondary, combineOperation)
			case other => throw new UnsupportedOperationException(s"Unsupported primary data type: $other")
		}
	}
	
	
	// OTHER    ----------------------------
	
	private def supportEntries(primary: DataType, combine: Set[DataType] = Set(), plus: Set[DataType] = Set(),
	                           minus: Set[DataType] = Set()): Seq[(DataType, ValueOperation2, Set[DataType])] =
	{
		if (combine.isEmpty)
			Pair((primary, Plus, plus), (primary, Minus, minus)).filterNot { _._3.isEmpty }
		else
			Pair((primary, Plus, combine ++ plus), (primary, Minus, combine ++ minus))
	}
	
	private def intAnd(primary: Int, secondary: Value, sign: Sign): Value = secondary.dataType match {
		case DoubleType => primary + (secondary.getDouble * sign.modifier)
		case FloatType => primary + (secondary.getFloat * sign.modifier)
		case LongType => primary + (secondary.getLong * sign.modifier)
		case _ => primary + (secondary.getInt * sign.modifier)
	}
	private def localDateAnd(primary: LocalDate, secondary: Value, sign: => Sign): Value =
		secondary.dataType match {
			case LocalDateType => primary - secondary.getLocalDate
			case LocalTimeType => primary + secondary.getLocalTime
			case LocalDateTimeType => primary.atStartOfDay - secondary.getLocalDateTime
			case DaysType => primary + (secondary.getDays * sign.modifier)
			case _ => primary + (secondary.getDuration * sign.modifier)
		}
	private def yearMonthAnd(primary: YearMonth, secondary: Value, sign: => Sign): Value =
		secondary.dataType match {
			case DaysType => primary.firstDay + secondary.getDays * sign.modifier
			case DurationType => primary.firstDay.atStartOfDay + secondary.getDuration * sign.modifier
			case LocalDateType => primary.firstDay - secondary.getLocalDate
			case _ => primary.firstDay - secondary.getYearMonth.firstDay
		}
	private def collectionAnd(primary: Seq[Value], secondary: Value, operation: CombineValuesOperation) =
		operation match {
			case Plus => primary ++ secondary.getVector
			case Minus =>
				secondary.dataType match {
					case PairType | VectorType =>
						val valuesToRemove = secondary.getVector.toSet
						if (valuesToRemove.isEmpty) primary else primary.filterNot(valuesToRemove.contains)
					case _ => primary.filterNot { _ ~== secondary }
				}
		}
}
