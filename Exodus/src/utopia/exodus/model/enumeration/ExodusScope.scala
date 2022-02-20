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
	val values = Vector[ExodusScope](ReadGeneralData, CreateUser, ReadPersonalData, PersonalActions,
		JoinOrganization, CreateOrganization, ReadOrganizationData, OrganizationActions, RequestPasswordReset,
		ChangeKnownPassword, ChangeEmail, TerminateOtherSessions, RevokeOtherTokens, DeleteAccount)
	
	
	// NESTED   ---------------------------
	
	/**
	  * Scope that allows read access to general data, such as available languages
	  */
	case object ReadGeneralData extends ExodusScope { override val id = 1 }
	/**
	  * Scope that allows user creation
	  */
	case object CreateUser extends ExodusScope { override val id = 2 }
	/**
	  * Scope that allows read access to user's personal data
	  */
	case object ReadPersonalData extends ExodusScope { override val id = 3 }
	/**
	  * Scope that allows read and write access to user's personal data
	  */
	case object PersonalActions extends ExodusScope { override val id = 4 }
	/**
	  * Scope that allows the user to join an organization / answer an organization invitation
	  */
	case object JoinOrganization extends ExodusScope { override val id = 5 }
	/**
	  * Scope that allows the user to create / start their own organization
	  */
	case object CreateOrganization extends ExodusScope { override val id = 6 }
	/**
	  * Scope that allows read access to organization data (in behalf of a user)
	  */
	case object ReadOrganizationData extends ExodusScope { override val id = 7 }
	/**
	  * Scope that allows organization actions (in behalf of a user)
	  */
	case object OrganizationActions extends ExodusScope { override val id = 8 }
	/**
	  * Scope that enables requesting a password reset (with email validation)
	  */
	case object RequestPasswordReset extends ExodusScope { override val id = 9 }
	/**
	  * Scope that enables replacing the user's password, but only when they know the previous password
	  */
	case object ChangeKnownPassword extends ExodusScope { override val id = 10 }
	/**
	  * Scope that enables replacing the user's password without requiring an existing password
	  */
	case object ReplaceForgottenPassword extends ExodusScope { override val id = 11 }
	/**
	  * Scope that enables the user to change their email address
	  */
	case object ChangeEmail extends ExodusScope { override val id = 12 }
	/**
	  * Scope that enables the user to log out from all devices and contexts.
	  * This doesn't however, allow the user to revoke other refresh- or permanent access tokens.
	  */
	case object TerminateOtherSessions extends ExodusScope { override val id = 13 }
	/**
	  * Scope that enables the user to revoke their other refresh- and access tokens
	  * (log out for all devices, revoke all refresh tokens & revoke all given access rights)
	  */
	case object RevokeOtherTokens extends ExodusScope { override val id = 14 }
	/**
	  * Scope that enables a user to delete their account and all personal data
	  */
	case object DeleteAccount extends ExodusScope { override val id = 15 }
}
