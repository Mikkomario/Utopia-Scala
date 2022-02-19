package utopia.exodus.model.enumeration

sealed trait ExodusTokenType extends TokenTypeIdWrapper

/**
  * An enumeration for exodus-originated token types
  * @author Mikko Hilpinen
  * @since 18.2.2022, v4.0
  */
object ExodusTokenType
{
	/**
	  * All token types introduced in Exodus
	  */
	val values = Vector[ExodusTokenType](ApiKey, SessionToken, RefreshToken,
		EmailValidatedSession, EmailValidationToken)
	
	/**
	  * Api keys are not tied to individual users and may be used to perform generic tasks,
	  * such as general data reading
	  */
	case object ApiKey extends ExodusTokenType
	{
		override def id = 1
	}
	/**
	  * Session tokens serve as standard temporary session authorizations.
	  * This may be tied to a single user and/or device.
	  */
	case object SessionToken extends ExodusTokenType
	{
		override def id = 2
	}
	/**
	  * Refresh tokens are used to refresh (user) sessions without needing to provide password information.
	  */
	case object RefreshToken extends ExodusTokenType
	{
		override def id = 3
	}
	/**
	  * Email validated sessions are based on email validation tokens, temporarily extending their capabilities.
	  */
	case object EmailValidatedSession extends ExodusTokenType
	{
		override def id = 4
	}
	/**
	  * Email validation tokens are sent through the email in attempt to validate the user's identity and/or their
	  * email address' validity.
	  */
	case object EmailValidationToken extends ExodusTokenType
	{
		override def id = 5
	}
}
