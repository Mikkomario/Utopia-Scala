package utopia.access.model.header

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.Mutate

/**
 * Stores a header value as text
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
class TextHeaderValue(override val text: String) extends MappableHeaderValue[String, TextHeaderValue]
{
	override def parsed: String = text
	override def toValue: Value = text
	
	override def map(f: Mutate[String]): TextHeaderValue = new TextHeaderValue(f(text))
}
