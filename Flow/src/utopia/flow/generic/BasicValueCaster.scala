package utopia.flow.generic

import utopia.flow.generic.ConversionReliability.{CONTEXT_LOSS, DANGEROUS, DATA_LOSS, MEANING_LOSS, PERFECT}
import ValueConversions._
import utopia.flow.collection.value.iterable.Pair
import utopia.flow.collection.value.typeless.{Model, Value}
import utopia.flow.parse.JSONReader
import utopia.flow.time.Days
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
    
    private val alternativeDateFormat = DateTimeFormatter.ofPattern("dd.MM.uuuu")
    
    override lazy val conversions = Set(
        // Any type can be converted to a string using .toString, although some conversions may be considered more
        // plausible
        Conversion(AnyType, StringType, DATA_LOSS),
        Conversion(IntType, StringType, CONTEXT_LOSS),
        Conversion(DoubleType, StringType, CONTEXT_LOSS),
        Conversion(LongType, StringType, CONTEXT_LOSS),
        Conversion(InstantType, StringType, CONTEXT_LOSS),
        Conversion(ModelType, StringType, CONTEXT_LOSS),
        // Conversions to Int
        Conversion(DoubleType, IntType, DATA_LOSS),
        Conversion(LongType, IntType, DATA_LOSS),
        Conversion(FloatType, IntType, DATA_LOSS),
        Conversion(BooleanType, IntType, CONTEXT_LOSS),
        Conversion(DaysType, IntType, CONTEXT_LOSS),
        Conversion(StringType, IntType, DANGEROUS),
        // Conversions to Double
        Conversion(IntType, DoubleType, PERFECT),
        Conversion(FloatType, DoubleType, PERFECT),
        Conversion(LongType, DoubleType, PERFECT),
        Conversion(DurationType, DoubleType, CONTEXT_LOSS),
        Conversion(StringType, DoubleType, DANGEROUS),
        // Conversions to Float
        Conversion(IntType, FloatType, PERFECT),
        Conversion(DoubleType, FloatType, DATA_LOSS),
        Conversion(LongType, FloatType, DATA_LOSS),
        Conversion(StringType, FloatType, DANGEROUS),
        // Conversions to Long
        Conversion(IntType, LongType, PERFECT),
        Conversion(DoubleType, LongType, DATA_LOSS),
        Conversion(FloatType, LongType, DATA_LOSS),
        Conversion(InstantType, LongType, DATA_LOSS),
        Conversion(DurationType, LongType, CONTEXT_LOSS),
        Conversion(DaysType, LongType, CONTEXT_LOSS),
        Conversion(StringType, LongType, DANGEROUS),
        // Conversions to Boolean
        Conversion(IntType, BooleanType, MEANING_LOSS),
        Conversion(StringType, BooleanType, DANGEROUS),
        // Conversions to Instant
        Conversion(LongType, InstantType, PERFECT),
        Conversion(LocalDateTimeType, InstantType, PERFECT),
        Conversion(DurationType, InstantType, MEANING_LOSS),
        Conversion(DaysType, InstantType, MEANING_LOSS),
        Conversion(StringType, InstantType, DANGEROUS),
        // Conversions to LocalDate
        Conversion(LocalDateTimeType, LocalDateType, DATA_LOSS),
        Conversion(DaysType, LocalDateType, MEANING_LOSS),
        Conversion(StringType, LocalDateType, DANGEROUS),
        // Conversions to LocalTime
        Conversion(LocalDateTimeType, LocalTimeType, DATA_LOSS),
        Conversion(DurationType, LocalTimeType, MEANING_LOSS),
        Conversion(StringType, LocalTimeType, DANGEROUS),
        // Conversions to LocalDateTime
        Conversion(InstantType, LocalDateTimeType, DATA_LOSS),
        Conversion(LocalDateType, LocalDateTimeType, PERFECT),
        Conversion(StringType, LocalDateTimeType, DANGEROUS),
        Conversion(PairType, LocalDateTimeType, DANGEROUS),
        // Conversions to Duration
        Conversion(DaysType, DurationType, PERFECT),
        Conversion(LocalTimeType, DurationType, CONTEXT_LOSS),
        Conversion(LongType, DurationType, PERFECT),
        Conversion(IntType, DurationType, PERFECT),
        Conversion(DoubleType, DurationType, PERFECT),
        Conversion(InstantType, DurationType, CONTEXT_LOSS),
        Conversion(ModelType, DurationType, DANGEROUS),
        Conversion(StringType, DurationType, DANGEROUS),
        // Conversions to Days
        Conversion(DurationType, DaysType, DATA_LOSS),
        Conversion(IntType, DaysType, PERFECT),
        Conversion(LocalDateType, DaysType, CONTEXT_LOSS),
        // Conversions to Vector
        Conversion(AnyType, VectorType, MEANING_LOSS),
        Conversion(PairType, VectorType, CONTEXT_LOSS),
        // Conversions to Pair
        Conversion(VectorType, PairType, DANGEROUS),
        Conversion(LocalDateTimeType, PairType, CONTEXT_LOSS),
        Conversion(LocalTimeType, PairType, CONTEXT_LOSS),
        Conversion(StringType, PairType, DANGEROUS),
        // Conversions to Model
        Conversion(DurationType, ModelType, CONTEXT_LOSS),
        Conversion(StringType, ModelType, DANGEROUS)
    )
    
    
    // IMPLEMENTED METHODS    ----
    
    override def cast(value: Value, toType: DataType) = 
    {
        val castResult: Option[Any] = toType match 
        {
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
    
    private def intOf(value: Value): Option[Int] = 
    {
        // Double, long, float and boolean can be cast to integers
        // String needs to be parsed
        value.dataType match 
        {
            case DoubleType => Some(value.getDouble.intValue())
            case LongType => Some(value.getLong.intValue())
            case FloatType => Some(value.getFloat.intValue())
            case BooleanType => Some(if (value.getBoolean) 1 else 0)
            case DaysType => Some(value.getDays.length)
            case StringType => Try { value.stringOr("0").toDouble.toInt }.toOption
            case _ => None
        }
    }
    
    private def doubleOf(value: Value): Option[Double] =
        value.dataType match 
        {
            case IntType => Some(value.getInt.toDouble)
            case LongType => Some(value.getLong.toDouble)
            case FloatType => Some(value.getFloat.toDouble)
            case DurationType => Some(value.getDuration.toPreciseMillis)
            case StringType =>
                value.string.map { _.replace(',', '.').trim }
                    .flatMap { s => Try { s.toDouble }.toOption }
            case _ => None
        }
    
    private def floatOf(value: Value): Option[Float] =
        value.dataType match 
        {
            case IntType => Some(value.getInt.toFloat)
            case LongType => Some(value.getLong.toFloat)
            case DoubleType => Some(value.getDouble.toFloat)
            case StringType => Try { value.stringOr("0").toFloat }.toOption
            case _ => None
        }
    
    private def longOf(value: Value): Option[Long] =
        value.dataType match 
        {
            case IntType => Some(value.getInt.toLong)
            case DoubleType => Some(value.getDouble.toLong)
            case FloatType => Some(value.getFloat.toLong)
            case InstantType => Some(value.getInstant.toEpochMilli)
            case DurationType => Some(value.getDuration.toMillis)
            case DaysType => Some(value.getDays.length.toLong)
            case StringType => Try { value.stringOr("0").toDouble.toLong }.toOption
            case _ => None
        }
    
    private def booleanOf(value: Value): Option[Boolean] =
        value.dataType match 
        {
            case IntType => Some(value.getInt != 0)
            case StringType =>
                val s = value.getString.toLowerCase
                if (s == "true")
                    Some(true)
                else if (s == "false")
                    Some(false)
                else if (s == "yes")
                    Some(true)
                else if (s == "no")
                    Some(false)
                else if (s == "y")
                    Some(true)
                else if (s == "n")
                    Some(false)
                else if (s == "1")
                    Some(true)
                else if (s == "0")
                    Some(false)
                else
                    None
            case _ => None
        }
    
    private def instantOf(value: Value): Option[Instant] =
        value.dataType match 
        {
            case LongType => Some(Instant.ofEpochMilli(value.getLong))
            case LocalDateTimeType =>
                val dateTime = value.getLocalDateTime
                Some(dateTime.toInstant(ZoneId.systemDefault().getRules.getOffset(dateTime)))
            case DurationType => Some(Instant.EPOCH + value.getDuration)
            case DaysType => Some(Instant.EPOCH + value.getDays)
            case StringType =>
                // Tries various parsing formats
                val str = value.getString
                Try { Instant.parse(str) }
                    .orElse { Try(ZonedDateTime.parse(str).toInstant) }
                    .orElse {
                        Try {
                            val localDateTime = LocalDateTime.parse(str)
                            localDateTime.toInstant(ZoneId.systemDefault().getRules.getOffset(localDateTime))
                        }
                    }.toOption
            case _ => None
        }
    
    private def localDateOf(value: Value): Option[LocalDate] =
        value.dataType match 
        {
            case LocalDateTimeType => Some(value.getLocalDateTime.toLocalDate)
            case DaysType => Some(LocalDate.ofEpochDay(value.getDays.length))
            case StringType =>
                val s = value.getString
                Try { LocalDate.parse(s) }.orElse { Try { LocalDate.parse(s, alternativeDateFormat) } }.toOption
            case _ => None
        }
    
    private def localTimeOf(value: Value): Option[LocalTime] =
        value.dataType match 
        {
            case DurationType => Some(LocalTime.MIDNIGHT + value.getDuration)
            case LocalDateTimeType => Some(value.getLocalDateTime.toLocalTime)
            case StringType => Try(LocalTime.parse(value.toString())).toOption
            case _ => None
        }
    
    private def localDateTimeOf(value: Value): Option[LocalDateTime] =
        value.dataType match 
        {
            case InstantType => Some(LocalDateTime.ofInstant(value.getInstant, ZoneId.systemDefault()))
            case LocalDateType => Some(value.getLocalDate.atStartOfDay())
            case StringType => Try(LocalDateTime.parse(value.toString())).toOption
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
    
    private def vectorOf(value: Value): Option[Vector[Value]] = value.dataType match
    {
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
            else if (s.contains('&')) {
                val (start, end) = s.splitAtFirst("&")
                Some(Pair[Value](start, end))
            }
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
        case StringType => JSONReader.apply(value.getString).toOption.filter { _.isOfType(ModelType) }.map { _.getModel }
        case _ => None
    }
    
    private def splitToValueVector(s: String, separator: Char) =
        s.split(separator).toVector.map { _.trim }.map { s =>
            if (s.isEmpty)
                Value.empty
            else
                Value(Some(s), StringType)
        }
}