package utopia.flow.parse.json

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConverterManager

/**
  * This object provides an interface that allows converting of values into valid JSON data
  * @author Mikko Hilpinen
  * @since 13.12.2016
  */
object JsonValueConverter extends ValueConverterManager(Single(BasicJsonValueConverter))
