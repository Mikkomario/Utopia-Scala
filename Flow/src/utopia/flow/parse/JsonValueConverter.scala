package utopia.flow.parse

/**
 * This object provides an interface that allows converting of values into valid JSON data
 * @author Mikko Hilpinen
 * @since 13.12.2016
 */
object JsonValueConverter extends ValueConverterManager(Vector(BasicJsonValueConverter))