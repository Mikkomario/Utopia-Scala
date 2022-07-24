package utopia.flow.util.logging

/**
 * This logging interface simply writes errors to System.err
 * @author Mikko Hilpinen
 * @since 8.6.2022, v1.16
 */
object SysErrLogger extends Logger
{
	override def apply(error: Option[Throwable], message: String) = {
		if (message.nonEmpty)
			System.err.println(message)
		error.foreach { _.printStackTrace() }
	}
}
