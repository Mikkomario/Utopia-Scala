package utopia.echo.model.vastai.process

import utopia.echo.model.vastai.instance.InstanceStatus

/**
 * An enumeration for different outcomes of API hosting on a Vast AI instance
 * @author Mikko Hilpinen
 * @since 27.02.2026, v1.5
 */
sealed trait ApiHostingResult
{
	/**
	 * @return Whether the API was hosted for some time
	 */
	def wasHosted: Boolean
}

object ApiHostingResult
{
	// VALUES   -------------------------
	
	/**
	 * Result given when the API couldn't be successfully set up
	 * @param cause Cause of this failure
	 */
	case class Failed(cause: Throwable) extends ApiHostingResult
	{
		override val wasHosted: Boolean = false
	}
	/**
	 * Result given when the API was successfully hosted, and was stopped without issues.
	 */
	case object Stopped extends ApiHostingResult
	{
		override val wasHosted: Boolean = true
	}
	/**
	 * Result given when the API was stopped because it could no longer be accessed
	 * @param instanceStatus Vast AI instance status at the time of stopping the API
	 */
	case class Disconnected(instanceStatus: InstanceStatus) extends ApiHostingResult
	{
		override val wasHosted: Boolean = true
	}
}
