package utopia.vault.database

import utopia.flow.parse.ValueConverter
import utopia.flow.generic.StringType
import utopia.flow.generic.DataType
import utopia.flow.datastructure.immutable.Value
import scala.collection.immutable.HashSet
import utopia.flow.generic.InstantType
import utopia.flow.generic.BooleanType
import utopia.flow.generic.IntType
import utopia.flow.generic.LongType
import utopia.flow.generic.FloatType
import utopia.flow.generic.DoubleType
import java.sql.Types
import java.sql.Timestamp
import utopia.flow.generic.LocalDateType
import java.sql.Date
import utopia.flow.generic.LocalTimeType
import java.sql.Time

/**
 * This value converter is able to convert some data types into sql compatible object types. The 
 * resulting objects are always accompanied with their respective sql type information. Please note
 * that since this converter uses instant type from java.time, it only supports JDBC drivers of 
 * 4.2 or later.
 * @author Mikko Hilpinen
 * @since 27.4.2017
 */
object BasicSqlValueConverter extends ValueConverter[(Any, Int)]
{
    override val supportedTypes: Set[DataType] = HashSet(StringType, InstantType, BooleanType, 
            IntType, LongType, FloatType, DoubleType, LocalDateType, LocalTimeType)
    
    override def apply(value: Value, dataType: DataType) = 
    {
        // NB: Instant into timestamp slot only works with JDBC 4.2 driver or later
        dataType match 
        {
            case DoubleType => (value.getDouble, Types.DOUBLE)
            case FloatType => (value.getFloat, Types.FLOAT)
            case LongType => (value.getLong, Types.BIGINT)
            case IntType => (value.getInt, Types.INTEGER)
            case BooleanType => (value.getBoolean, Types.BOOLEAN)
            case InstantType => (Timestamp.from(value.getInstant), Types.TIMESTAMP)
            case LocalDateType => (Date.valueOf(value.getLocalDate), Types.DATE)
            case LocalTimeType => (Time.valueOf(value.getLocalTime), Types.TIME)
            case _ => (value.getString, Types.VARCHAR)
        }
    }
}