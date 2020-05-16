package utopia.flow.generic

import scala.collection.immutable.HashSet
import utopia.flow.generic.ConversionReliability.PERFECT
import utopia.flow.generic.ConversionReliability.DATA_LOSS
import utopia.flow.generic.ConversionReliability.DANGEROUS
import utopia.flow.generic.ConversionReliability.MEANING_LOSS
import utopia.flow.datastructure.immutable.Value
import java.time.{Instant, LocalDate, LocalDateTime, LocalTime, ZoneId, ZonedDateTime}

import scala.util.Try

/**
 * This value caster handles the basic data types
 * @author Mikko Hilpinen
 * @since 19.11.2016
 */
object BasicValueCaster extends ValueCaster
{
    // ATTRIBUTES    --------------
    
    override lazy val conversions = HashSet(
            Conversion(AnyType, StringType, DATA_LOSS), 
            Conversion(DoubleType, IntType, DATA_LOSS), 
            Conversion(LongType, IntType, DATA_LOSS), 
            Conversion(FloatType, IntType, DATA_LOSS), 
            Conversion(BooleanType, IntType, PERFECT), 
            Conversion(StringType, IntType, DANGEROUS), 
            Conversion(IntType, DoubleType, PERFECT), 
            Conversion(FloatType, DoubleType, PERFECT), 
            Conversion(LongType, DoubleType, PERFECT), 
            Conversion(StringType, DoubleType, DANGEROUS), 
            Conversion(IntType, FloatType, PERFECT), 
            Conversion(DoubleType, FloatType, DATA_LOSS), 
            Conversion(LongType, FloatType, DATA_LOSS), 
            Conversion(StringType, FloatType, DANGEROUS), 
            Conversion(IntType, LongType, PERFECT), 
            Conversion(DoubleType, LongType, DATA_LOSS), 
            Conversion(FloatType, LongType, DATA_LOSS), 
            Conversion(StringType, LongType, DANGEROUS), 
            Conversion(InstantType, LongType, DATA_LOSS), 
            Conversion(IntType, BooleanType, MEANING_LOSS),  
            Conversion(StringType, BooleanType, MEANING_LOSS), 
            Conversion(LongType, InstantType, PERFECT), 
            Conversion(LocalDateTimeType, InstantType, PERFECT), 
            Conversion(StringType, InstantType, DANGEROUS), 
            Conversion(LocalDateTimeType, LocalDateType, DATA_LOSS), 
            Conversion(StringType, LocalDateType, DANGEROUS), 
            Conversion(LocalDateTimeType, LocalTimeType, DATA_LOSS), 
            Conversion(StringType, LocalTimeType, DANGEROUS), 
            Conversion(InstantType, LocalDateTimeType, DATA_LOSS), 
            Conversion(LocalDateType, LocalDateTimeType, PERFECT), 
            Conversion(StringType, LocalDateTimeType, DANGEROUS), 
            Conversion(AnyType, VectorType, MEANING_LOSS))
    
    
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
            case VectorType => vectorOf(value)
            case _ => None
        }
        
        castResult.map { objValue => new Value(Some(objValue), toType) }
    }
    
    
    // OTHER METHODS    ---------
    
    private def stringOf(value: Value): Option[String] = 
    {
        value.dataType match 
        {
            // Vectors have a special formatting like "[a, b, c, d]" 
            // This is in order to form JSON -compatible output
            case VectorType => 
                val vector = value.vectorOr()
                if (vector.isEmpty) Some("[]") else Some(s"[${ vector.map { _.toJson }.reduceLeft { _ + ", " + _ } }]")
            case _ => value.content.map { _.toString() }
        }
    }
    
    private def intOf(value: Value): Option[Int] = 
    {
        // Double, long, float and boolean can be cast to integers
        // String needs to be parsed
        value.dataType match 
        {
            case DoubleType => Some(value.doubleOr().intValue())
            case LongType => Some(value.longOr().intValue())
            case FloatType => Some(value.floatOr().intValue())
            case BooleanType => Some(if (value.booleanOr()) 1 else 0)
            case StringType => Try(value.stringOr("0").toDouble.toInt).toOption
            case _ => None
        }
    }
    
    private def doubleOf(value: Value): Option[Double] = 
    {
        value.dataType match 
        {
            case IntType => Some(value.intOr().toDouble)
            case LongType => Some(value.longOr().toDouble)
            case FloatType => Some(value.floatOr().toDouble)
            case StringType => Try(value.stringOr("0").toDouble).toOption
            case _ => None
        }
    }
    
    private def floatOf(value: Value): Option[Float] = 
    {
        value.dataType match 
        {
            case IntType => Some(value.intOr().toFloat)
            case LongType => Some(value.longOr().toFloat)
            case DoubleType => Some(value.doubleOr().toFloat)
            case StringType => Try(value.stringOr("0").toFloat).toOption
            case _ => None
        }
    }
    
    private def longOf(value: Value): Option[Long] = 
    {
        value.dataType match 
        {
            case IntType => Some(value.intOr().toLong)
            case DoubleType => Some(value.doubleOr().toLong)
            case FloatType => Some(value.floatOr().toLong)
            case InstantType => Some(value.instantOr().getEpochSecond)
            case StringType => Try(value.stringOr("0").toDouble.toLong).toOption
            case _ => None
        }
    }
    
    private def booleanOf(value: Value): Option[Boolean] = 
    {
        value.dataType match 
        {
            case IntType => Some(value.intOr() != 0)
            case StringType => Some(value.stringOr().toLowerCase() == "true")
            case _ => None
        }
    }
    
    private def instantOf(value: Value): Option[Instant] = 
    {
        value.dataType match 
        {
            case LongType => Some(Instant.ofEpochSecond(value.getLong))
            case StringType =>
                // Tries various parsing formats
                val str = value.getString
                Try(Instant.parse(str)).orElse(Try(ZonedDateTime.parse(str).toInstant)).orElse
                {
                    Try
                    {
                        val localDateTime = LocalDateTime.parse(str)
                        localDateTime.toInstant(ZoneId.systemDefault().getRules.getOffset(localDateTime))
                    }
                    
                }.toOption
                
            case LocalDateTimeType =>
                val dateTime = value.getLocalDateTime
                Some(dateTime.toInstant(ZoneId.systemDefault().getRules.getOffset(dateTime)))
            case _ => None
        }
    }
    
    private def localDateOf(value: Value): Option[LocalDate] = 
    {
        value.dataType match 
        {
            case LocalDateTimeType => Some(value.localDateTimeOr().toLocalDate)
            case StringType => Try(LocalDate.parse(value.toString())).toOption
            case _ => None
        }
    }
    
    private def localTimeOf(value: Value): Option[LocalTime] = 
    {
        value.dataType match 
        {
            case LocalDateTimeType => Some(value.localDateTimeOr().toLocalTime)
            case StringType => Try(LocalTime.parse(value.toString())).toOption
            case _ => None
        }
    }
    
    private def localDateTimeOf(value: Value): Option[LocalDateTime] = 
    {
        value.dataType match 
        {
            case InstantType => Some(LocalDateTime.ofInstant(value.instantOr(), ZoneId.systemDefault()))
            case LocalDateType => Some(value.localDateOr().atStartOfDay())
            case StringType => Try(LocalDateTime.parse(value.toString())).toOption
            case _ => None
        }
    }
    
    private def vectorOf(value: Value) = Some(Vector(value))
}