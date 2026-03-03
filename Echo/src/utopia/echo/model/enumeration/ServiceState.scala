package utopia.echo.model.enumeration

/**
 * An enumeration for different (default / starting) states of an external service.
 * @author Mikko Hilpinen
 * @since 03.03.2026, v1.5
 */
sealed trait ServiceState
{
	// ABSTRACT -------------------------
	
	/**
	 * @return Whether the service has already been installed
	 */
	def isInstalled: Boolean
	/**
	 * @return Whether the service has already been started
	 */
	def hasStarted: Boolean
	
	
	// COMPUTED -------------------------
	
	/**
	 * @return Whether the service has not yet been installed
	 */
	def wasNotInstalled = !isInstalled
	/**
	 * @return Whether the service has yet to be started
	 */
	def hasNotStarted = !hasStarted
}

object ServiceState
{
	// VALUES   -------------------------
	
	/**
	 * State where the service is not present / installed
	 */
	case object NotInstalled extends ServiceState
	{
		override val isInstalled: Boolean = false
		override val hasStarted: Boolean = false
	}
	/**
	 * State where the service is installed / available, but has not been started
	 */
	case object NotStarted extends ServiceState
	{
		override val isInstalled: Boolean = true
		override val hasStarted: Boolean = false
	}
	/**
	 * state where the service has already been started
	 */
	case object Running extends ServiceState
	{
		override val isInstalled: Boolean = true
		override val hasStarted: Boolean = true
	}
}
