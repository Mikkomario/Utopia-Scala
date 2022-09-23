package utopia.vault.database

import utopia.flow.collection.value.typeless.Value

import java.sql.Types
import utopia.flow.generic.{BooleanType, DataType, DoubleType, FloatType, IntType, LongType, StringType}

import java.sql.Date
import java.sql.Timestamp
import utopia.flow.generic.ValueConversions._

import java.sql.Time

/**
 * This generator handles value generation for the basic data types introduced in the Flow project. 
 * @author Mikko Hilpinen
 * @since 28.4.2017
 */
object BasicSqlValueGenerator extends SqlValueGenerator
{
    // IMPLEMENTED METHODS    ---------------
    
    override def apply(value: Any, sqlType: Int) = 
    {
        // Type mappings looked up from:
        // https://www.service-architecture.com/articles/database/mapping_sql_and_java_data_types.html
        sqlType match 
        {
            case Types.TIMESTAMP | Types.TIMESTAMP_WITH_TIMEZONE => 
                Some(value.asInstanceOf[Timestamp].toInstant())
            case Types.DATE => Some(value.asInstanceOf[Date].toLocalDate())
            case Types.TIME => Some(value.asInstanceOf[Time].toLocalTime())
            case Types.INTEGER | Types.SMALLINT | Types.TINYINT => wrap(value, IntType)
            case Types.DOUBLE | Types.FLOAT => wrap(value, DoubleType)
            case Types.VARCHAR | Types.CHAR | Types.LONGNVARCHAR => wrap(value, StringType)
            case Types.BOOLEAN | Types.BIT => wrap(value, BooleanType)
            case Types.BIGINT => wrap(value, LongType)
            case Types.REAL => wrap(value, FloatType)
            case Types.NUMERIC | Types.DECIMAL => Some(value.asInstanceOf[java.math.BigDecimal].doubleValue())
            
            case _ => None
        }
    }
    
    
    // OTHER METHODS    ---------------------
    
    private def wrap(value: Any, toType: DataType) = Some(new Value(Some(value), toType))
}