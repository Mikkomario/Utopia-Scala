package utopia.exodus.database

import utopia.citadel.database.access.single.user.DbSingleUser
import utopia.exodus.database.access.single.user.DbUserPassword
import utopia.vault.database.Connection

/**
  * An object containing extensions for user access points
  * @author Mikko Hilpinen
  * @since 26.6.2021, v2.0
  */
object UserDbExtensions
{
	implicit class ExtendedSingleDbUser(val a: DbSingleUser) extends AnyVal
	{
		/**
		  * @return An access point to this user's password information
		  */
		def password = DbUserPassword.ofUserWithId(a.id)
		/**
		  * Updates the password of this user
		  * @param newPassword New password to assign (will be hashed)
		  * @param connection Implicit DB Connection
		  * @return Inserted user password
		  */
		def password_=(newPassword: String)(implicit connection: Connection) =
			password.update(newPassword)
	}
}
