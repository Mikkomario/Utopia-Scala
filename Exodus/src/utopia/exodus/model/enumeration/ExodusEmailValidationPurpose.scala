package utopia.exodus.model.enumeration

/**
  * An enumeration for email validation purposes introduced in Exodus project
  * @author Mikko Hilpinen
  * @since 21.2.2022, v4.0
  */
sealed trait ExodusEmailValidationPurpose extends EmailValidationPurposeIdWrapper

object ExodusEmailValidationPurpose
{
	/**
	  * All Exodus email validation purpose values
	  */
	val values = Vector[ExodusEmailValidationPurpose](UserCreation, EmailChange, PasswordReset, OrganizationInvitation)
	
	/**
	  * Represents email validations sent in order to validate a new user's email address
	  */
	case object UserCreation extends ExodusEmailValidationPurpose { override val id = 1 }
	/**
	  * Represents email validations sent in order to validate a user's new email (when changing email)
	  */
	case object EmailChange extends ExodusEmailValidationPurpose { override val id = 2 }
	/**
	  * Represents email validations sent in order to recover / reset a user's forgotten password
	  */
	case object PasswordReset extends ExodusEmailValidationPurpose { override val id = 3 }
	/**
	  * Represents email validations sent when inviting a user into an organization
	  */
	case object OrganizationInvitation extends ExodusEmailValidationPurpose { override val id = 4 }
}