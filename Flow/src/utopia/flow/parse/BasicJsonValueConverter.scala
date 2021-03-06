package utopia.flow.parse

import scala.collection.immutable.HashSet
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.{BooleanType, DataType, DoubleType, FloatType, InstantType, IntType, LocalDateType, LongType, ModelType, StringType, VectorType}

/**
 * This json value writer is able to write instances of basic data types into json
 * @author Mikko Hilpinen
 * @since 17.12.2016
 */
object BasicJsonValueConverter extends ValueConverter[String]
{
    override val supportedTypes = HashSet(StringType, VectorType, ModelType, IntType, DoubleType,
            FloatType, LongType, BooleanType, InstantType)
    
    override def apply(value: Value, dataType: DataType) = 
    {
        dataType match 
        {
            case StringType => "\"" + value.getString.replace("\"", "'")
                .replace("\\", "\\\\") + "\""
            case VectorType => s"[${value.getVector.map { _.toJson }.mkString(", ")}]"
            case ModelType => value.getModel.toJson
            // Handles instant type separately to format it correctly
            case InstantType => "\"" + value.getString + "\""
            // Same treatment is given to dates
            case LocalDateType => "\"" + value.getString + "\""
            case _ => value.getString
        }
    }
}