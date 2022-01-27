package utopia.exodus.model.enumeration

/**
  * An enumeration for the default email validation purposes
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
sealed trait StandardEmailValidationPurpose
{
	/**
	  * Id of this purpose in the database
	  */
	val id: Int
}

object StandardEmailValidationPurpose
{
	/**
	  * Represents email validation when it is used for creating a new user
	  */
	case object UserCreation extends StandardEmailValidationPurpose
	{
		override val id = 1
	}
	
	/**
	  * Represents email validation when it is used for authenticating password reset
	  */
	case object PasswordReset extends StandardEmailValidationPurpose
	{
		override val id = 2
	}
	
	/**
	  * Represents email validation when it is performed for an authenticated user in order to change
	  * their email address
	  */
	case object EmailChange extends StandardEmailValidationPurpose
	{
		override val id = 3
	}
	
	/**
	  * Represents an email validation that is sent along with an organization invitation
	  */
	case object OrganizationInvitation extends StandardEmailValidationPurpose
	{
		override val id = 4
	}
}
