package utopia.flow.parse.json

import utopia.flow.generic.casting.ValueConverter
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType._
import utopia.flow.util.StringExtensions._

import scala.collection.immutable.HashSet

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
		dataType match {
			case StringType =>
				s"\"${ value.getString
					.escapeBackSlashes
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
				}\""
			case VectorType => s"[${ value.getVector.map { _.toJson }.mkString(", ") }]"
			case ModelType => value.getModel.toJson
			// Handles instant type separately to format it correctly
			case InstantType => s"\"${ value.getString }\""
			// Same treatment is given to dates
			case LocalDateType => s"\"${ value.getString }\""
			case _ => value.getString
		}
	}
}
