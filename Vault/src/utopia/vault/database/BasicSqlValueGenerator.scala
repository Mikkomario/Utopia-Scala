package utopia.vault.database

import java.sql.Types
import java.sql.Date
import java.sql.Timestamp
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType.{BooleanType, DoubleType, FloatType, IntType, LongType, StringType}

import java.sql.Time

/**
  * This generator handles value generation for the basic data types introduced in the Flow project.
  * @author Mikko Hilpinen
  * @since 28.4.2017
  */
object BasicSqlValueGenerator extends SqlValueGenerator
{
	// IMPLEMENTED    ---------------
	
	override def apply(value: Any, sqlType: Int) = {
		// Type mappings looked up from:
		// https://www.service-architecture.com/articles/database/mapping_sql_and_java_data_types.html
		sqlType match {
			case Types.TIMESTAMP | Types.TIMESTAMP_WITH_TIMEZONE => Some(fromTimestamp(value))
			case Types.DATE => Some(fromDate(value))
			case Types.TIME => Some(fromTime(value))
			case Types.INTEGER | Types.SMALLINT | Types.TINYINT => Some(fromInt(value))
			case Types.DOUBLE | Types.FLOAT => Some(fromDouble(value))
			case Types.VARCHAR | Types.CHAR | Types.LONGNVARCHAR => Some(fromString(value))
			case Types.BOOLEAN | Types.BIT => Some(fromBoolean(value))
			case Types.BIGINT => Some(fromLong(value))
			case Types.REAL => Some(fromFloat(value))
			case Types.NUMERIC | Types.DECIMAL => Some(fromBigDecimal(value))
			case _ => None
		}
	}
	
	// WET WET
	override def conversionFrom(sqlType: Int): Option[Any => Value] = sqlType match {
		case Types.TIMESTAMP | Types.TIMESTAMP_WITH_TIMEZONE => Some(fromTimestamp)
		case Types.DATE => Some(fromDate)
		case Types.TIME => Some(fromTime)
		case Types.INTEGER | Types.SMALLINT | Types.TINYINT => Some(fromInt)
		case Types.DOUBLE | Types.FLOAT => Some(fromDouble)
		case Types.VARCHAR | Types.CHAR | Types.LONGNVARCHAR => Some(fromString)
		case Types.BOOLEAN | Types.BIT => Some(fromBoolean)
		case Types.BIGINT => Some(fromLong)
		case Types.REAL => Some(fromFloat)
		case Types.NUMERIC | Types.DECIMAL => Some(fromBigDecimal)
		case _ => None
	}
	
	
	// OTHER    ---------------------
	
	private def fromTimestamp(value: Any): Value = value.asInstanceOf[Timestamp].toInstant
	private def fromDate(value: Any): Value = value.asInstanceOf[Date].toLocalDate
	private def fromTime(value: Any): Value = value.asInstanceOf[Time].toLocalTime
	private def fromInt(value: Any) = wrap(value, IntType)
	private def fromDouble(value: Any) = wrap(value, DoubleType)
	private def fromString(value: Any) = wrap(value, StringType)
	private def fromBoolean(value: Any) = wrap(value, BooleanType)
	private def fromLong(value: Any) = wrap(value, LongType)
	private def fromFloat(value: Any) = wrap(value, FloatType)
	private def fromBigDecimal(value: Any): Value = value.asInstanceOf[java.math.BigDecimal].doubleValue
	
	private def wrap(value: Any, toType: DataType) = new Value(Some(value), toType)
}