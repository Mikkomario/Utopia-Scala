package utopia.flow.parse

import scala.collection.immutable.HashSet
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.{BooleanType, DataType, DoubleType, FloatType, InstantType, IntType, LongType, ModelType, StringType, VectorType}

/**
 * This JSON value writer is able to write instances of basic data types into JSON
 * @author Mikko Hilpinen
 * @since 17.12.2016
 */
object BasicJSONValueConverter extends ValueConverter[String]
{
    override val supportedTypes = HashSet(StringType, VectorType, ModelType, IntType, DoubleType,
            FloatType, LongType, BooleanType, InstantType)
    
    override def apply(value: Value, dataType: DataType) = 
    {
        dataType match 
        {
            case StringType => "\"" + value.getString.replaceAll("\"", "'") + "\""
            case VectorType => s"[${value.getVector.map { _.toJSON }.mkString(", ")}]"
            case ModelType => value.getModel.toJSON
            // Handles instant type separately to format it correctly
            case InstantType => "\"" + value.getString + "\""
            case _ => value.getString
        }
    }
}