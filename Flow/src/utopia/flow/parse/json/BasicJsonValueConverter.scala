package utopia.flow.parse.json

import utopia.flow.collection.CollectionExtensions.RichIterableOnce
import utopia.flow.generic.casting.ValueConverter
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType
import utopia.flow.generic.model.mutable.DataType._
import utopia.flow.util.StringExtensions._

/**
  * This json value writer is able to write instances of basic data types into json
  * @author Mikko Hilpinen
  * @since 17.12.2016
  */
object BasicJsonValueConverter extends ValueConverter[JsonAppendix]
{
	// ATTRIBUTES	---------------------
	
	override val supportedTypes =
		Set(StringType, VectorType, ModelType, IntType, DoubleType, FloatType, LongType, BooleanType, InstantType,
			LocalDateType)
			
	
	// IMPLEMENTED	----------------------
	
	override def apply(value: Value, dataType: DataType) = dataType match {
		// Case: String type => Wraps the content within double quotes and escapes special characters
		case StringType =>
			JsonAppendix { (builder, _) =>
				builder += '\"'
				builder ++= value.getString
					.escapeBackSlashes
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
				builder += '\"'
			}
		// Case: Vector type => Wraps the content within square brackets and separates the values with commas
		case VectorType =>
			JsonAppendix { (builder, depth) =>
				builder += '['
				value.getVector.nonEmptyIterator.foreach { valuesIter =>
					valuesIter.next().appendToJson(builder, depth)
					valuesIter.foreach { v =>
						builder += ','
						builder += ' '
						v.appendToJson(builder, depth)
					}
				}
				builder += ']'
			}
		// Case: Model type => Uses the model's own JSON conversion
		case ModelType => JsonAppendix.wrap(value.getModel)
		// Case: Timestamp => Converts it to a string
		case InstantType | LocalDateType =>
			JsonAppendix { (builder, _) =>
				builder += '\"'
				builder ++= value.getString
				builder += '\"'
			}
		// Other types (numbers & boolean) can be converted to string directly
		case _ => JsonAppendix { (builder, _) => builder ++= value.getString }
	}
}
