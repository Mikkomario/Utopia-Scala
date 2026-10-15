package utopia.flow.parse.json

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConverterManager
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.mutable.DataType.StringType

/**
  * This object provides an interface that allows converting of values into valid JSON data
  * @author Mikko Hilpinen
  * @since 13.12.2016
  */
object JsonValueConverter extends ValueConverterManager[JsonAppendix](Single(BasicJsonValueConverter))
{
	// Empty strings are not converted to null
	override def apply(value: Value) = {
		if (value.isEmpty && value.dataType == StringType)
			Some(JsonAppendix { (builder, _) => builder ++= "\"\"" })
		else
			super.apply(value)
	}
}