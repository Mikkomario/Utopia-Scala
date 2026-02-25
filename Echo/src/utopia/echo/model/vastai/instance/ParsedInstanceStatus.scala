package utopia.echo.model.vastai.instance

object ParsedInstanceStatus
{
	/**
	 * @param raw An instance status as a String
	 * @return Parsed instance status
	 */
	def apply(raw: String): ParsedInstanceStatus = apply(InstanceState.forStatus(raw), raw)
}

/**
 * Used for recording an instance's status, including the value received from Vast AI
 * @param value The interpreted / parsed status value
 * @param raw The raw status value
 * @author Mikko Hilpinen
 * @since 24.02.2026, v1.5
 */
case class ParsedInstanceStatus(value: InstanceState, raw: String)
{
	override def toString: String = raw
}