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
		case StringType =>
			JsonAppendix { (builder, _) =>
				builder += '\"'
				builder ++= value.getString
					.escapeBackSlashes
					.replace("\"", "\\\"")
					.replace("\n", "\\n")
				builder += '\"'
			}
		case VectorType =>
			JsonAppendix { (builder, depth) =>
				builder += '['
				value.getVector.nonEmptyIterator.foreach { valuesIter =>
					valuesIter.next().appendToJson(builder, depth)
					value.getVector.foreach { v =>
						builder += ','
						builder += ' '
						v.appendToJson(builder, depth)
					}
				}
				builder += ']'
			}
		case ModelType => JsonAppendix.wrap(value.getModel)
		// Handles instant type separately to format it correctly
		// Same treatment is given to dates
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
