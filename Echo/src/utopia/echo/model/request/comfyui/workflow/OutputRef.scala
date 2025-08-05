package utopia.echo.model.request.comfyui.workflow

import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.generic.casting.ValueConversions._

/**
 * Used for referencing the output of another workflow node
 *
 * @author Mikko Hilpinen
 * @since 04.08.2025, v1.4
 */
case class OutputRef(source: String, index: Int = 0) extends ValueConvertible
{
	override def toValue: Value = Pair[Value](source, index)
}
