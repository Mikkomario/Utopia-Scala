package utopia.flow.generic.casting

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.{Pair, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.enumeration.ConversionReliability._
import utopia.flow.generic.model.immutable.{Conversion, Model, Value}
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType._
import utopia.flow.parse.json.{JsonParser, JsonReader}
import utopia.flow.parse.string.Regex
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.TimeUnit.{Day, Hour, MilliSecond, Minute, Second}
import utopia.flow.time._
import utopia.flow.util.StringExtensions._

import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success, Try}

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
		DateTimeFormatter.ofPattern("MM/dd/uuuu")
	)
	private lazy val supportedLocalDateTimeFormats = Vector(
		DateTimeFormatter.ISO_LOCAL_DATE_TIME,
		DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm:ss"),
		DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm:ss"),
		DateTimeFormatter.ofPattern("dd.MM.uuuu HH:mm"),
		DateTimeFormatter.ofPattern("MM/dd/uuuu HH:mm")
	)
	private lazy val dateTimeSplitRegex = Regex("T") || Regex.whitespace
	
	override lazy val conversions = Set(
		// Any type can be converted to a string using .toString, although some conversions may be considered more
		// plausible
		Conversion(AnyType, StringType, DataLoss),
		Conversion(IntType, StringType, ContextLoss),
		Conversion(DoubleType, StringType, ContextLoss),
		Conversion(LongType, StringType, ContextLoss),
		Conversion(InstantType, StringType, ContextLoss),
		Conversion(MonthType, StringType, ContextLoss),
		Conversion(ModelType, StringType, ContextLoss),
		// Conversions to Int
		Conversion(DoubleType, IntType, DataLoss),
		Conversion(LongType, IntType, DataLoss),
		Conversion(FloatType, IntType, DataLoss),
		Conversion(BooleanType, IntType, ContextLoss),
		Conversion(DaysType, IntType, ContextLoss),
		Conversion(YearType, IntType, ContextLoss),
		Conversion(MonthType, IntType, ContextLoss),
		Conversion(StringType, IntType, Dangerous),
		// Conversions to Double
		Conversion(IntType, DoubleType, Perfect),
		Conversion(FloatType, DoubleType, Perfect),
		Conversion(LongType, DoubleType, Perfect),
		Conversion(DurationType, DoubleType, ContextLoss),
		Conversion(StringType, DoubleType, Dangerous),
		// Conversions to Float
		Conversion(IntType, FloatType, Perfect),
		Conversion(DoubleType, FloatType, DataLoss),
		Conversion(LongType, FloatType, DataLoss),
		Conversion(StringType, FloatType, Dangerous),
		// Conversions to Long
		Conversion(IntType, LongType, Perfect),
		Conversion(DoubleType, LongType, DataLoss),
		Conversion(FloatType, LongType, DataLoss),
		Conversion(InstantType, LongType, DataLoss),
		Conversion(DurationType, LongType, ContextLoss),
		Conversion(DaysType, LongType, ContextLoss),
		Conversion(StringType, LongType, Dangerous),
		// Conversions to Boolean
		Conversion(IntType, BooleanType, MeaningLoss),
		Conversion(StringType, BooleanType, Dangerous),
		// Conversions to Instant
		Conversion(LongType, InstantType, Perfect),
		Conversion(LocalDateTimeType, InstantType, Perfect),
		Conversion(DurationType, InstantType, MeaningLoss),
		Conversion(DaysType, InstantType, MeaningLoss),
		Conversion(StringType, InstantType, Dangerous),
		// Conversions to LocalDate
		Conversion(LocalDateTimeType, LocalDateType, DataLoss),
		Conversion(DaysType, LocalDateType, MeaningLoss),
		Conversion(YearMonthType, LocalDateType, ContextLoss),
		Conversion(YearType, LocalDateType, MeaningLoss),
		Conversion(StringType, LocalDateType, Dangerous),
		// Conversions to LocalTime
		Conversion(LocalDateTimeType, LocalTimeType, DataLoss),
		Conversion(DurationType, LocalTimeType, MeaningLoss),
		Conversion(StringType, LocalTimeType, Dangerous),
		// Conversions to LocalDateTime
		Conversion(InstantType, LocalDateTimeType, DataLoss),
		Conversion(LocalDateType, LocalDateTimeType, Perfect),
		Conversion(LocalTimeType, LocalDateTimeType, MeaningLoss),
		Conversion(StringType, LocalDateTimeType, Dangerous),
		Conversion(PairType, LocalDateTimeType, Dangerous),
		// Conversions to Duration
		Conversion(DaysType, DurationType, Perfect),
		Conversion(LocalTimeType, DurationType, ContextLoss),
		Conversion(LongType, DurationType, Perfect),
		Conversion(IntType, DurationType, Perfect),
		Conversion(DoubleType, DurationType, Perfect),
		Conversion(InstantType, DurationType, ContextLoss),
		Conversion(ModelType, DurationType, Dangerous),
		Conversion(StringType, DurationType, Dangerous),
		// Conversions to Days
		Conversion(DurationType, DaysType, DataLoss),
		Conversion(IntType, DaysType, Perfect),
		Conversion(LocalDateType, DaysType, ContextLoss),
		Conversion(MonthType, DaysType, MeaningLoss),
		Conversion(YearType, DaysType, MeaningLoss),
		Conversion(YearMonthType, DaysType, MeaningLoss),
		// Conversions to Year
		Conversion(StringType, YearType, Dangerous),
		Conversion(IntType, YearType, Perfect),
		Conversion(LocalDateType, YearType, DataLoss),
		Conversion(YearMonthType, YearType, DataLoss),
		// Conversions to Month
		Conversion(StringType, MonthType, Dangerous),
		Conversion(IntType, MonthType, Dangerous),
		Conversion(LocalDateType, MonthType, DataLoss),
		Conversion(YearMonthType, MonthType, DataLoss),
		// Conversions to YearMonth
		Conversion(StringType, YearMonthType, Dangerous),
		Conversion(LocalDateType, YearMonthType, DataLoss),
		Conversion(MonthType, YearMonthType, MeaningLoss),
		Conversion(PairType, YearMonthType, Dangerous),
		Conversion(ModelType, YearMonthType, Dangerous),
		// Conversions to Vector
		Conversion(AnyType, VectorType, MeaningLoss),
		Conversion(PairType, VectorType, ContextLoss),
		// Conversions to Pair
		Conversion(VectorType, PairType, Dangerous),
		Conversion(LocalDateTimeType, PairType, ContextLoss),
		Conversion(LocalTimeType, PairType, ContextLoss),
		Conversion(YearMonthType, PairType, ContextLoss),
		Conversion(StringType, PairType, Dangerous),
		// Conversions to Model
		Conversion(DurationType, ModelType, ContextLoss),
		Conversion(StringType, ModelType, Dangerous),
		Conversion(YearMonthType, ModelType, ContextLoss)
	)
	
	/**
	  * The JSON parser used by this interface (variable).
	  */
	var jsonParser: JsonParser = JsonReader
	
	
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
			case YearType => yearOf(value)
			case MonthType => monthOf(value)
			case YearMonthType => yearMonthOf(value)
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
			case YearType => Some(value.getYear.value)
			case MonthType => Some(value.getMonth.value)
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
			case YearType => Some(value.getYear.firstDay)
			case YearMonthType => Some(value.getYearMonth.firstDay)
			case StringType => stringToDate(value.getString)
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
	
	private def durationOf(value: Value): Option[Duration] = value.dataType match {
		case DaysType => Some(value.getDays.toDuration)
		case LocalTimeType => Some(value.getLocalTime.toDuration)
		case LongType => Some(value.getLong.millis)
		case IntType => Some(value.getInt.millis)
		case DoubleType => Some(value.getDouble.millis)
		case InstantType => Some(value.getInstant - Instant.EPOCH)
		case ModelType =>
			val model = value.getModel
			model("value").long.flatMap { v =>
				model("unit").string.flatMap(timeUnitFrom).map { Duration(v, _) }
			}
		case StringType =>
			val s = value.getString
			val firstLetterIndex = s.indexWhere { _.isLetter }
			if (firstLetterIndex < 0)
				s.double.map { _.millis }
			else {
				val (numPart, unitPart) = s.splitAt(firstLetterIndex)
				timeUnitFrom(unitPart.trim)
					.flatMap { unit => numPart.trim.long.map { Duration(_, unit) } }
			}
		case _ => None
	}
	
	private def timeUnitFrom(str: String) = {
		str.toLowerCase match {
			case "ms" | "millis" | "milliseconds" => Some(MilliSecond)
			case "s" | "sec" | "seconds" => Some(Second)
			case "m" | "min" | "mins" | "minutes" => Some(Minute)
			case "h" | "hour" | "hours" => Some(Hour)
			case "d" | "day" | "days" => Some(Day)
			case _ => None
		}
	}
	
	private def daysOf(value: Value): Option[Days] = value.dataType match {
		case DurationType => Some(Days(value.getDuration.toDays.toInt))
		case IntType => Some(Days(value.getInt))
		case LocalDateType => Some(Days(value.getLocalDate.toEpochDay.toInt))
		case YearType => Some(value.getYear.length)
		case YearMonthType => Some(value.getYearMonth.length)
		case MonthType => Some(value.getMonth.length)
		case _ => None
	}
	
	private def yearOf(value: Value): Option[Year] = value.dataType match {
		case StringType =>
			val str = value.getString
			str.toIntOption match {
				case Some(int) => Some(Year(int))
				case None => stringToDate(str).map { _.year }
			}
		case IntType => Some(Year(value.getInt))
		case LocalDateType => Some(value.getLocalDate.year)
		case YearMonthType => Some(value.getYearMonth.year)
		case _ => None
	}
	private def monthOf(value: Value): Option[Month] = value.dataType match {
		case IntType => Month.findForValue(value.getInt)
		case StringType =>
			val str = value.getString
			stringToMonth(str).orElse { stringToDate(str).map { _.month } }
		case LocalDateType => Some(value.getLocalDate.month)
		case YearMonthType => Some(value.getYearMonth.month)
		case _ => None
	}
	private def yearMonthOf(value: Value): Option[YearMonth] = value.dataType match {
		case StringType =>
			val str = value.getString
			val parts = {
				if (str.contains('/'))
					str.split('/').toVector
				else if (str.contains('.'))
					str.split('.').toVector
				else
					Single(str)
			}
			parts.size match {
				case 1 =>
					stringToDate(str) match {
						case Some(date) => Some(date.yearMonth)
						case None => stringToMonth(str).map { Today.year/_ }
					}
				
				case 2 => stringToMonth(parts.head).flatMap { month => parts(1).toIntOption.map { Year(_)/month } }
				case _ =>
					Pair(1, 0).findMap { i => stringToMonth(parts(i)) }.flatMap { month =>
						parts(2).toIntOption.map { Year(_)/month }
					}
			}
		case LocalDateType => Some(value.getLocalDate.yearMonth)
		case MonthType => Some(Today.year/value.getMonth)
		case PairType =>
			val p = value.getPair
			p.first.year.flatMap { year => p.second.month.map(year.apply) }.orElse {
				p.second.year.flatMap { year => p.first.month.map(year.apply) }
			}
		case ModelType =>
			val m = value.getModel
			m("year", "y").year.flatMap { year => m("month", "mo", "m").month.map(year.apply) }
			
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
		case YearMonthType =>
			val ym = value.getYearMonth
			Some(Pair(ym.year, ym.month))
		case VectorType =>
			val v = value.getVector
			if (v.hasSize >= 2) Some(Pair(v.head, v(1))) else None
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
			val duration = value.getDuration
			duration.tryLength match {
				case Success((amount, unit)) =>
					val unitString = unit match {
						case MilliSecond => Some("ms")
						case Second => Some("s")
						case Minute => Some("m")
						case Hour => Some("h")
						case Day => Some("d")
						case _ => None
					}
					val len = unitString match {
						case Some(_) => amount
						case None => MilliSecond.countIn(amount, unit)
					}
					Some(Model.from("value" -> len, "unit" -> unitString.getOrElse[String]("ms")))
				case _ => None
			}
		case YearMonthType =>
			val ym = value.getYearMonth
			Some(Model.from("year" -> ym.year, "month" -> ym.month))
			
		case StringType => jsonParser.apply(value.getString).toOption.filter { _.isOfType(ModelType) }.map { _.getModel }
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
		// Removes any leading and trailing spaces and other control characters
		val cleanStr = str.stripControlCharacters.trim
		
		if (cleanStr.isEmpty)
			None
		else if (cleanStr.contains(','))
			Try { fromDecimal(cleanStr.replace(',', '.')) }.toOption
		else if (cleanStr.contains('.'))
			Try { fromDecimal(cleanStr) }.toOption
		else
			Try { fromIntegral(cleanStr) }.toOption
	}
	
	private def stringToDate(str: String) =
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
	private def stringToMonth(str: String) = str.toIntOption match {
		case Some(int) => Month.findForValue(int)
		case None => Month.findForName(str)
	}
}
