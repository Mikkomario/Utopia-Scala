package utopia.flow.generic

/**
 * These exceptions are thrown when a function is used without first setting up the required
 * environment. The exception message should explain, how to setup the environment.
 * @author Mikko Hilpinen
 * @since 10.12.2016
 */
case class EnvironmentNotSetupException(message: String, cause: Throwable = null)
	extends RuntimeException(message, cause)