package utopia.journey.model.enumeration

/**
  * An enumeration representing different reasons why user credentials may be requested
  * @author Mikko Hilpinen
  * @since 20.6.2020, v1
  */
sealed trait AuthorizationReason

object AuthorizationReason
{
	/**
	  * Used when no user credentials are available and some are required
	  */
	case object FirstLogin extends AuthorizationReason
	
	/**
	  * Used when previous user session was expired or cannot be used for some other reason
	  */
	case object SessionExpired extends AuthorizationReason
	
	/**
	  * Used when previously provided credentials couldn't be authorized
	  */
	case object AuthorizationFailed extends AuthorizationReason
}
