package utopia.flow.util.logging

import utopia.flow.generic.model.immutable.Model

/**
 * This logging interface simply writes errors to System.err
 * @author Mikko Hilpinen
 * @since 8.6.2022, v1.16
 */
object SysErrLogger extends Logger
{
	// COMPUTED -------------------------
	
	/**
	  * @return A System.err logger version that includes timestamps
	  */
	def includingTime = TimedSysErrLogger
	
	
	// IMPLEMENTED  ---------------------
	
	override def apply(error: Option[Throwable], message: String, details: Model): Unit = {
		if (message.nonEmpty)
			System.err.println(message)
		details.propertiesIterator.foreach { detail =>
			System.err.println(s"\t- ${ detail.name }: ${ detail.value }")
		}
		error.foreach { _.printStackTrace() }
	}
}
