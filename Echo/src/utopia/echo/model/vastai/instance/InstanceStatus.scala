package utopia.echo.model.vastai.instance

import utopia.flow.generic.factory.SureFromModelFactory
import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties

object InstanceStatus extends SureFromModelFactory[InstanceStatus]
{
	override def parseFrom(model: HasProperties): InstanceStatus = {
		def get(key: String) = ParsedInstanceStatus(model(key).getString)
		apply(get("actual_status"), get("intended_status"), get("cur_state"), get("next_state"),
			model("status_msg").getString)
	}
}

/**
 * Combines information concerning an instance's status
 * @param actual Current status of the instance container
 * @param intended Intended status of the instance container (e.g. "running" or "stopped")
 * @param current Current state of the machine contract
 * @param next Next scheduled state for the machine contract
 * @param message Status message for the instance
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class InstanceStatus(actual: ParsedInstanceStatus, intended: ParsedInstanceStatus,
                          current: ParsedInstanceStatus, next: ParsedInstanceStatus, message: String = "")
{
	override def toString: String = if (message.isEmpty) actual.toString else s"$actual: $message"
}