package utopia.exodus.model.enumeration

/**
  * An enumeration for Exodus-originated scopes
  * @author Mikko Hilpinen
  * @since 18.2.2022, v4.0
  */
sealed trait ExodusScope extends ScopeIdWrapper

object ExodusScope
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * All Exodus scope values
	  */
	val values = Vector[ExodusScope](GeneralDataRead, UserCreation, PersonalDataRead, PersonalActions,
		OrganizationDataRead, OrganizationActions, PasswordReset, EmailChange, AccountDeletion)
	
	
	// NESTED   ---------------------------
	
	/**
	  * Scope that allows read access to general data, such as available languages
	  */
	case object GeneralDataRead extends ExodusScope
	{
		override val id = 1
	}
	/**
	  * Scope that allows user creation
	  */
	case object UserCreation extends ExodusScope
	{
		override val id = 2
	}
	/**
	  * Scope that allows read access to user's personal data
	  */
	case object PersonalDataRead extends ExodusScope
	{
		override val id = 3
	}
	/**
	  * Scope that allows read and write access to user's personal data
	  */
	case object PersonalActions extends ExodusScope
	{
		override val id = 4
	}
	/**
	  * Scope that allows read access to organization data (in behalf of a user)
	  */
	case object OrganizationDataRead extends ExodusScope
	{
		override val id = 5
	}
	/**
	  * Scope that allows organization actions (in behalf of a user)
	  */
	case object OrganizationActions extends ExodusScope
	{
		override val id = 6
	}
	/**
	  * Scope that enables replacing the user's password
	  */
	case object PasswordReset extends ExodusScope
	{
		override val id = 7
	}
	/**
	  * Scope that enables the user to change their email address
	  */
	case object EmailChange extends ExodusScope
	{
		override val id = 8
	}
	/**
	  * Scope that enables a user to delete their account and all personal data
	  */
	case object AccountDeletion extends ExodusScope
	{
		override val id = 9
	}
}
