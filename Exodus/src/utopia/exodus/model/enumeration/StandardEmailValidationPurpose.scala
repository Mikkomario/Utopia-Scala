package utopia.exodus.model.enumeration

/**
  * An enumeration for the default email validation purposes
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
@deprecated("This will be removed in a future release", "v4.0")
sealed trait StandardEmailValidationPurpose
{
	/**
	  * Id of this purpose in the database
	  */
	val id: Int
}

@deprecated("This will be removed in a future release", "v4.0")
object StandardEmailValidationPurpose
{
	/**
	  * Represents email validation when it is used for creating a new user
	  */
	@deprecated("This will be removed in a future release", "v4.0")
	case object UserCreation extends StandardEmailValidationPurpose
	{
		override val id = 1
	}
	
	/**
	  * Represents email validation when it is used for authenticating password reset
	  */
	@deprecated("This will be removed in a future release", "v4.0")
	case object PasswordReset extends StandardEmailValidationPurpose
	{
		override val id = 2
	}
	
	/**
	  * Represents email validation when it is performed for an authenticated user in order to change
	  * their email address
	  */
	@deprecated("This will be removed in a future release", "v4.0")
	case object EmailChange extends StandardEmailValidationPurpose
	{
		override val id = 3
	}
	
	/**
	  * Represents an email validation that is sent along with an organization invitation
	  */
	@deprecated("This will be removed in a future release", "v4.0")
	case object OrganizationInvitation extends StandardEmailValidationPurpose
	{
		override val id = 4
	}
}
