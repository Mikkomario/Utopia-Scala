package utopia.flow.parse

import scala.collection.immutable.HashMap
import utopia.flow.generic.DataType
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ConversionHandler

/**
 * This object provides an interface that allows converting of values into valid JSON data
 * @author Mikko Hilpinen
 * @since 13.12.2016
 */
object JSONValueConverter extends ValueConverterManager(Vector(BasicJSONValueConverter))