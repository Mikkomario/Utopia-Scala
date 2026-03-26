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
	
	/**
	 * @return If API setup failed, yields the associated error
	 */
	def failure: Option[Throwable]
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
		// ATTRIBUTES   -----------------
		
		override val wasHosted: Boolean = false
		
		
		// IMPLEMENTED  -----------------
		
		override def failure: Option[Throwable] = Some(cause)
	}
	/**
	 * Result given when the API was successfully hosted, and was stopped without issues.
	 */
	case object Stopped extends ApiHostingResult
	{
		// ATTRIBUTES   -----------------
		
		override val wasHosted: Boolean = true
		
		
		// IMPLEMENTED  -----------------
		
		override def failure: Option[Throwable] = None
	}
	/**
	 * Result given when the API was stopped because it could no longer be accessed
	 * @param instanceStatus Vast AI instance status at the time of stopping the API
	 */
	case class Disconnected(instanceStatus: InstanceStatus) extends ApiHostingResult
	{
		// ATTRIBUTES   -----------------
		
		override val wasHosted: Boolean = true
		
		
		// IMPLEMENTED  -----------------
		
		override def failure: Option[Throwable] = None
	}
}
