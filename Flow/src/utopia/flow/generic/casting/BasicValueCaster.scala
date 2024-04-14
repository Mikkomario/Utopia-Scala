package utopia.flow.generic.casting

import utopia.flow.collection.immutable.Pair
import ValueConversions._
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.{Conversion, Model, Value}
import utopia.flow.generic.model.enumeration.ConversionReliability.{ContextLoss, Dangerous, DataLoss, MeaningLoss, Perfect}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType.{AnyType, BooleanType, DaysType, DoubleType, DurationType, FloatType, InstantType, IntType, LocalDateTimeType, LocalDateType, LocalTimeType, LongType, ModelType, PairType, StringType, VectorType}
import utopia.flow.parse.json.JsonReader
import utopia.flow.parse.string.Regex
import utopia.flow.time.{Days, Today}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._

import java.time.format.DateTimeFormatter
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZonedDateTime}
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.{DAYS, FiniteDuration, HOURS, MILLISECONDS, MINUTES, SECONDS}
import scala.util.Try

/**
  * This value caster handles the basic data types
  * @author Mikko Hilpinen
  * @since 19.11.2016
  */
object BasicValueCaster extends ValueCaster
{
	// ATTRIBUTES    --------------
	
	private lazy val supportedLocalDateFormats = Vector(
		DateTimeFormatter.ISO_LOCAL_DATE,
		DateTimeFormatter.ofPattern("dd.MM.uuuu"),
		DateTimeFormatter.ofPattern("MM/dd/uuuu"))
	private lazy val supportedLocalDateTimeFormats = Vector(
		DateTimeFormatter.ISO_LOCAL_DATE_TIME,
		DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss"),
		DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss"),
		DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm"),
		DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm"))
	private lazy val dateTimeSplitRegex = Regex("T") || Regex.whiteSpace
	
	override lazy val conversions = Set(
		// Any type can be converted to a string using .toString, although some conversions may be considered more
		// plausible
		Conversion(AnyType, StringType, DataLoss),
		immutable.Conversion(IntType, StringType, ContextLoss),
		immutable.Conversion(DoubleType, StringType, ContextLoss),
		immutable.Conversion(LongType, StringType, ContextLoss),
		immutable.Conversion(InstantType, StringType, ContextLoss),
		immutable.Conversion(ModelType, StringType, ContextLoss),
		// Conversions to Int
		immutable.Conversion(DoubleType, IntType, DataLoss),
		immutable.Conversion(LongType, IntType, DataLoss),
		immutable.Conversion(FloatType, IntType, DataLoss),
		immutable.Conversion(BooleanType, IntType, ContextLoss),
		immutable.Conversion(DaysType, IntType, ContextLoss),
		immutable.Conversion(StringType, IntType, Dangerous),
		// Conversions to Double
		immutable.Conversion(IntType, DoubleType, Perfect),
		immutable.Conversion(FloatType, DoubleType, Perfect),
		immutable.Conversion(LongType, DoubleType, Perfect),
		immutable.Conversion(DurationType, DoubleType, ContextLoss),
		immutable.Conversion(StringType, DoubleType, Dangerous),
		// Conversions to Float
		immutable.Conversion(IntType, FloatType, Perfect),
		immutable.Conversion(DoubleType, FloatType, DataLoss),
		immutable.Conversion(LongType, FloatType, DataLoss),
		immutable.Conversion(StringType, FloatType, Dangerous),
		// Conversions to Long
		immutable.Conversion(IntType, LongType, Perfect),
		immutable.Conversion(DoubleType, LongType, DataLoss),
		immutable.Conversion(FloatType, LongType, DataLoss),
		immutable.Conversion(InstantType, LongType, DataLoss),
		immutable.Conversion(DurationType, LongType, ContextLoss),
		immutable.Conversion(DaysType, LongType, ContextLoss),
		immutable.Conversion(StringType, LongType, Dangerous),
		// Conversions to Boolean
		immutable.Conversion(IntType, BooleanType, MeaningLoss),
		immutable.Conversion(StringType, BooleanType, Dangerous),
		// Conversions to Instant
		immutable.Conversion(LongType, InstantType, Perfect),
		immutable.Conversion(LocalDateTimeType, InstantType, Perfect),
		immutable.Conversion(DurationType, InstantType, MeaningLoss),
		immutable.Conversion(DaysType, InstantType, MeaningLoss),
		immutable.Conversion(StringType, InstantType, Dangerous),
		// Conversions to LocalDate
		immutable.Conversion(LocalDateTimeType, LocalDateType, DataLoss),
		immutable.Conversion(DaysType, LocalDateType, MeaningLoss),
		immutable.Conversion(StringType, LocalDateType, Dangerous),
		// Conversions to LocalTime
		immutable.Conversion(LocalDateTimeType, LocalTimeType, DataLoss),
		immutable.Conversion(DurationType, LocalTimeType, MeaningLoss),
		immutable.Conversion(StringType, LocalTimeType, Dangerous),
		// Conversions to LocalDateTime
		immutable.Conversion(InstantType, LocalDateTimeType, DataLoss),
		immutable.Conversion(LocalDateType, LocalDateTimeType, Perfect),
		immutable.Conversion(LocalTimeType, LocalDateTimeType, MeaningLoss),
		immutable.Conversion(StringType, LocalDateTimeType, Dangerous),
		immutable.Conversion(PairType, LocalDateTimeType, Dangerous),
		// Conversions to Duration
		immutable.Conversion(DaysType, DurationType, Perfect),
		immutable.Conversion(LocalTimeType, DurationType, ContextLoss),
		immutable.Conversion(LongType, DurationType, Perfect),
		immutable.Conversion(IntType, DurationType, Perfect),
		immutable.Conversion(DoubleType, DurationType, Perfect),
		immutable.Conversion(InstantType, DurationType, ContextLoss),
		immutable.Conversion(ModelType, DurationType, Dangerous),
		immutable.Conversion(StringType, DurationType, Dangerous),
		// Conversions to Days
		immutable.Conversion(DurationType, DaysType, DataLoss),
		immutable.Conversion(IntType, DaysType, Perfect),
		immutable.Conversion(LocalDateType, DaysType, ContextLoss),
		// Conversions to Vector
		immutable.Conversion(AnyType, VectorType, MeaningLoss),
		immutable.Conversion(PairType, VectorType, ContextLoss),
		// Conversions to Pair
		immutable.Conversion(VectorType, PairType, Dangerous),
		immutable.Conversion(LocalDateTimeType, PairType, ContextLoss),
		immutable.Conversion(LocalTimeType, PairType, ContextLoss),
		immutable.Conversion(StringType, PairType, Dangerous),
		// Conversions to Model
		immutable.Conversion(DurationType, ModelType, ContextLoss),
		immutable.Conversion(StringType, ModelType, Dangerous)
	)
	
	
	// IMPLEMENTED METHODS    ----
	
	override def cast(value: Value, toType: DataType) =
	{
		val castResult: Option[Any] = toType match {
			// Any object can be cast into a string
			case StringType => stringOf(value)
			case IntType => intOf(value)
			case DoubleType => doubleOf(value)
			case FloatType => floatOf(value)
			case LongType => longOf(value)
			case BooleanType => booleanOf(value)
			case InstantType => instantOf(value)
			case LocalDateType => localDateOf(value)
			case LocalTimeType => localTimeOf(value)
			case LocalDateTimeType => localDateTimeOf(value)
			case DurationType => durationOf(value)
			case DaysType => daysOf(value)
			case VectorType => vectorOf(value)
			case PairType => pairOf(value)
			case ModelType => modelOf(value)
			case _ => None
		}
		
		castResult.map { objValue => new Value(Some(objValue), toType) }
	}
	
	
	// OTHER METHODS    ---------
	
	private def stringOf(value: Value): Option[String] =
		value.dataType match {
			// Vectors have a special formatting like "[a, b, c, d]"
			// This is in order to form JSON -compatible output
			case VectorType =>
				val vector = value.getVector
				Some(if (vector.isEmpty) "[]" else s"[${ vector.map { _.toJson }.mkString(", ") }]")
			// Durations use a custom .toString defined in TimeExtensions
			case DurationType => Some(value.getDuration.description)
			case _ => value.content.map { _.toString() }
		}
	
	private def intOf(value: Value): Option[Int] = {
		// Double, long, float and boolean can be cast to integers
		// String needs to be parsed
		value.dataType match {
			case DoubleType => Some(value.getDouble.intValue())
			case LongType => Some(value.getLong.intValue())
			case FloatType => Some(value.getFloat.intValue())
			case BooleanType => Some(if (value.getBoolean) 1 else 0)
			case DaysType => Some(value.getDays.length)
			case StringType => value.string.flatMap { stringToNumber(_) { _.toDouble.toInt } { _.toInt } }
			case _ => None
		}
	}
	private def doubleOf(value: Value): Option[Double] =
		value.dataType match {
			case IntType => Some(value.getInt.toDouble)
			case LongType => Some(value.getLong.toDouble)
			case FloatType => Some(value.getFloat.toDouble)
			case DurationType => Some(value.getDuration.toPreciseMillis)
			case StringType => value.string.flatMap { stringToNumber(_) { _.toDouble } { _.toDouble } }
			case _ => None
		}
	private def floatOf(value: Value): Option[Float] =
		value.dataType match {
			case IntType => Some(value.getInt.toFloat)
			case LongType => Some(value.getLong.toFloat)
			case DoubleType => Some(value.getDouble.toFloat)
			case StringType => value.string.flatMap { stringToNumber(_) { _.toFloat } { _.toFloat } }
			case _ => None
		}
	private def longOf(value: Value): Option[Long] =
		value.dataType match {
			case IntType => Some(value.getInt.toLong)
			case DoubleType => Some(value.getDouble.toLong)
			case FloatType => Some(value.getFloat.toLong)
			case InstantType => Some(value.getInstant.toEpochMilli)
			case DurationType => Some(value.getDuration.toMillis)
			case DaysType => Some(value.getDays.length.toLong)
			case StringType => value.string.flatMap { stringToNumber(_) { _.toDouble.toLong } { _.toLong } }
			case _ => None
		}
	
	private def booleanOf(value: Value): Option[Boolean] =
		value.dataType match {
			case IntType => Some(value.getInt != 0)
			case StringType =>
				value.getString.toLowerCase match {
					case "true" => Some(true)
					case "false" => Some(false)
					case "yes" => Some(true)
					case "no" => Some(false)
					case "y" => Some(true)
					case "n" => Some(false)
					case "1" => Some(true)
					case "0" => Some(false)
					case _ => None
				}
			case _ => None
		}
	
	private def instantOf(value: Value): Option[Instant] =
		value.dataType match {
			case LongType => Some(Instant.ofEpochMilli(value.getLong))
			case LocalDateTimeType =>
				val dateTime = value.getLocalDateTime
				Some(dateTime.toInstant(ZoneId.systemDefault().getRules.getOffset(dateTime)))
			case DurationType => Some(Instant.EPOCH + value.getDuration)
			case DaysType => Some(Instant.EPOCH + value.getDays)
			case StringType =>
				// Tries various parsing formats
				val str = value.getString
				// Priority 1: Instant format
				Try { Instant.parse(str) }
					// Prio 2: Zoned date time format
					.orElse { Try(ZonedDateTime.parse(str).toInstant) }
					.toOption
					// Prio 3: Local date time format
					.orElse {
						supportedLocalDateTimeFormats
							.findMap { f => Try { LocalDateTime.parse(str, f) }.toOption }
							.map { local => local.toInstant(ZoneId.systemDefault().getRules.getOffset(local)) }
					}
					// Prio 4: Local date format
					.orElse {
						supportedLocalDateFormats
							.findMap { f => Try { LocalDate.parse(str, f) }.toOption }
							.map { date =>
								val dateTime = date.atStartOfDay()
								dateTime.toInstant(ZoneId.systemDefault().getRules.getOffset(dateTime))
							}
					}
			case _ => None
		}
	private def localDateOf(value: Value): Option[LocalDate] =
		value.dataType match {
			case LocalDateTimeType => Some(value.getLocalDateTime.toLocalDate)
			case DaysType => Some(LocalDate.ofEpochDay(value.getDays.length))
			case StringType =>
				val str = value.getString
				supportedLocalDateFormats.findMap { f => Try { LocalDate.parse(str, f) }.toOption }
					// Backup strategy: Extract the date portion from the input string
					.orElse {
						dateTimeSplitRegex.startIndexIteratorIn(str).nextOption().filter { _ > 0 }
							.flatMap { timeStartIndex =>
								val datePart = str.take(timeStartIndex)
								// WET WET
								supportedLocalDateFormats.findMap { f => Try { LocalDate.parse(datePart, f) }.toOption }
							}
					}
			case _ => None
		}
	private def localTimeOf(value: Value): Option[LocalTime] =
		value.dataType match {
			case DurationType => Some(LocalTime.MIDNIGHT + value.getDuration)
			case LocalDateTimeType => Some(value.getLocalDateTime.toLocalTime)
			case StringType => Try(LocalTime.parse(value.toString())).toOption
			case _ => None
		}
	private def localDateTimeOf(value: Value): Option[LocalDateTime] =
		value.dataType match {
			case InstantType => Some(LocalDateTime.ofInstant(value.getInstant, ZoneId.systemDefault()))
			case LocalDateType => Some(value.getLocalDate.atStartOfDay())
			case LocalTimeType => Some(Today.atTime(value.getLocalTime))
			case StringType =>
				val str = value.getString
				// Prio 1: Directly readable as local date time
				supportedLocalDateTimeFormats.findMap { f => Try { LocalDateTime.parse(str, f) }.toOption }
					// Prio 2: Converts from an instant (UTC) time to local time
					.orElse { Try { Instant.parse(str).toLocalDateTime }.toOption }
					// Prio 2: Converts from zoned time to local time
					.orElse { Try { ZonedDateTime.parse(str).toInstant.toLocalDateTime }.toOption }
					// Prio 3: Converts a local date to a local date time
					.orElse {
						supportedLocalDateFormats
							.findMap { f => Try { LocalDate.parse(str, f).atStartOfDay() }.toOption }
					}
				
			case PairType =>
				val p = value.getPair
				p.first.localDate.flatMap { date => p.second.localTime.map { LocalDateTime.of(date, _) } }
			case _ => None
		}
	
	private def durationOf(value: Value): Option[FiniteDuration] = value.dataType match {
		case DaysType => Some(value.getDays.toDuration)
		case LocalTimeType => Some(value.getLocalTime.toDuration)
		case LongType => Some(value.getLong.millis)
		case IntType => Some(value.getInt.millis)
		case DoubleType => Some(value.getDouble.millis)
		case InstantType => Some(value.getInstant - Instant.EPOCH)
		case ModelType =>
			val model = value.getModel
			model("value").long.flatMap { v =>
				model("unit").string.flatMap(timeUnitFrom).map { FiniteDuration(v, _) }
			}
		case StringType =>
			val s = value.getString
			val firstLetterIndex = s.indexWhere { _.isLetter }
			if (firstLetterIndex < 0)
				s.double.map { _.millis }
			else {
				val (numPart, unitPart) = s.splitAt(firstLetterIndex)
				timeUnitFrom(unitPart.trim)
					.flatMap { unit => numPart.trim.double.map { a => FiniteDuration(a.toLong, unit) } }
			}
		case _ => None
	}
	
	private def timeUnitFrom(str: String) = {
		str.toLowerCase match {
			case "ms" | "millis" | "milliseconds" => Some(TimeUnit.MILLISECONDS)
			case "s" | "sec" | "seconds" => Some(TimeUnit.SECONDS)
			case "m" | "min" | "mins" | "minutes" => Some(TimeUnit.MINUTES)
			case "h" | "hour" | "hours" => Some(TimeUnit.HOURS)
			case "d" | "day" | "days" => Some(TimeUnit.DAYS)
			case _ => None
		}
	}
	
	private def daysOf(value: Value): Option[Days] = value.dataType match {
		case DurationType => Some(Days(value.getDuration.toDays.toInt))
		case IntType => Some(Days(value.getInt))
		case LocalDateType => Some(Days(value.getLocalDate.toEpochDay.toInt))
		case _ => None
	}
	
	private def vectorOf(value: Value): Option[Vector[Value]] = value.dataType match {
		case PairType => Some(value.getPair.toVector)
		case StringType =>
			val s = value.getString
			if ((s.startsWith("[") && s.endsWith("]")) || (s.startsWith("(") && s.endsWith(")")))
				Some(splitToValueVector(s.drop(1).dropRight(1), ','))
			else if (s.contains(','))
				Some(splitToValueVector(s, ','))
			else if (s.contains(';'))
				Some(splitToValueVector(s, ';'))
			else
				Some(Vector(value))
		case _ => Some(Vector(value))
	}
	
	private def pairOf(value: Value): Option[Pair[Value]] = value.dataType match {
		case LocalDateTimeType =>
			val dt = value.getLocalDateTime
			Some(Pair[Value](dt.toLocalDate, dt.toLocalTime))
		case LocalTimeType =>
			val t = value.getLocalTime
			Some(Pair(t.getHour, t.getMinute))
		case VectorType =>
			val v = value.getVector
			if (v.size >= 2) Some(Pair(v.head, v(1))) else None
		case StringType =>
			val s = value.getString
			val containsComma = s.contains(',')
			if (containsComma && ((s.startsWith("(") && s.endsWith(")")) || (s.startsWith("[") && s.endsWith("]")))) {
				val v = splitToValueVector(s.drop(1).dropRight(1), ',')
				Some(Pair(v.head, v(1)))
			}
			else if (s.contains('&'))
				Some(s.splitAtFirst("&").map { s => s: Value })
			else if (containsComma) {
				val v = splitToValueVector(s, ',')
				Some(Pair(v.head, v(1)))
			}
			else if (s.contains(';')) {
				val v = splitToValueVector(s, ';')
				Some(Pair(v.head, v(1)))
			}
			else
				None
		case _ => None
	}
	
	private def modelOf(value: Value): Option[Model] = value.dataType match {
		case DurationType =>
			val d = value.getDuration
			val unitString = d.unit match {
				case MILLISECONDS => Some("ms")
				case SECONDS => Some("s")
				case MINUTES => Some("m")
				case HOURS => Some("h")
				case DAYS => Some("d")
				case _ => None
			}
			val len = unitString match {
				case Some(_) => d.length
				case None => d.toMillis
			}
			Some(Model.from("value" -> len, "unit" -> unitString.getOrElse[String]("ms")))
		case StringType => JsonReader.apply(value.getString).toOption.filter { _.isOfType(ModelType) }.map { _.getModel }
		case _ => None
	}
	
	private def splitToValueVector(s: String, separator: Char) =
		s.split(separator).toVector.map { _.trim }.map { s =>
			if (s.isEmpty)
				Value.empty
			else
				Value(Some(s), StringType)
		}
		
	// Handles both the decimal and integral number form options
	private def stringToNumber[N](str: String)(fromDecimal: String => N)(fromIntegral: String => N): Option[N] = {
		if (str.isEmpty)
			None
		else if (str.contains(','))
			Try { fromDecimal(str.replace(',', '.')) }.toOption
		else if (str.contains('.'))
			Try { fromDecimal(str) }.toOption
		else
			Try { fromIntegral(str) }.toOption
	}
}
