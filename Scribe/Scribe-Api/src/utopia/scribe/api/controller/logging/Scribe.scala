package utopia.scribe.api.controller.logging

import utopia.flow.util.logging.Logger
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Unrecoverable

object Scribe
{
	
}

/**
  * A logging implementation that utilizes the Scribe features
  * @author Mikko Hilpinen
  * @since 22.5.2023, v0.1
  * @param context A string representation of the context in which this logger serves
  * @param defaultSeverity The default level of [[Severity]] recorded by this logger (default = Unrecoverable)
  */
// TODO: Add logToConsole and logToFile -options (to ScribeContext) once the basic features have been implemented
case class Scribe(context: String, defaultSeverity: Severity = Unrecoverable) extends Logger
{
	// IMPLEMENTED  -------------------------
	
	def apply(error: Option[Throwable], message: String) = ???
}
