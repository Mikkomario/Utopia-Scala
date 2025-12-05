package utopia.access.model.header

import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Mutate
import utopia.flow.view.immutable.View

/**
 * A header value which wraps a generic [[Value]]
 * @author Mikko Hilpinen
 * @since 04.12.2025, v1.7
 */
class GenericHeaderValue(override val value: Value)
	extends MappableHeaderValue[Value, GenericHeaderValue] with View[Value]
{
	override def text: String = value.getString
	override def parsed: Value = value
	override def toValue: Value = value
	
	override def map(f: Mutate[Value]): GenericHeaderValue = new GenericHeaderValue(f(value))
}
