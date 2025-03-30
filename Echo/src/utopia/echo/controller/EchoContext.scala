package utopia.echo.controller

import utopia.access.model.enumeration.Status.InternalServerError
import utopia.access.model.enumeration.Status

/**
  * Used for specifying and accessing Echo-specific context variables
  * @author Mikko Hilpinen
  * @since 19.07.2024, v1.0
  */
object EchoContext
{
	var parseFailureStatus: Status = InternalServerError
}
