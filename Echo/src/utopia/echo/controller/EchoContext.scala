package utopia.echo.controller

import utopia.access.model.enumeration.Status
import utopia.annex.model.response.Response

/**
  * Used for specifying and accessing Echo-specific context variables
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
@deprecated("Deprecated for removal", "v1.5")
object EchoContext
{
	@deprecated("Deprecated for removal", "v1.5")
	def parseFailureStatus: Status = Response.parseFailureStatus
	@deprecated("Deprecated for removal", "v1.5")
	def parseFailureStatus_=(status: Status) = Response.parseFailureStatus = status
}
