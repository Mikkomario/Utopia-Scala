package utopia.exodus.database

import utopia.citadel.database.access.single.user.DbSingleUser
import utopia.exodus.database.access.single.user.DbUserPassword
import utopia.exodus.database.model.user.UserAuthModel
import utopia.exodus.util.PasswordHash
import utopia.vault.database.Connection
import utopia.vault.sql.{Select, Where}

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
		
		/**
		  * @param connection DB Connection (implicit)
		  * @return Password hash for this user. None if no hash was found.
		  */
		@deprecated("Please use password.hash instead", "v3.0")
		def passwordHash(implicit connection: Connection) =
			connection(Select(UserAuthModel.table, UserAuthModel.hashAttName) +
				Where(UserAuthModel.withUserId(a.id).toCondition)).firstValue.string
		
		/**
		  * Checks whether the specified password matches this user's current password
		  * @param password Password to test
		  * @param connection DB Connection (implicit)
		  * @return Whether the specified password is this user's current password
		  */
		@deprecated("Please use password.test(String) instead", "v3.0")
		def checkPassword(password: String)(implicit connection: Connection) =
			passwordHash.exists { PasswordHash.validatePassword(password, _) }
		
		/**
		  * Updates this user's password
		  * @param newPassword New password for this user
		  * @param connection DB Connection (implicit)
		  * @return Whether any user's password was actually affected
		  */
		@deprecated("Please use password = String instead", "v3.0")
		def changePassword(newPassword: String)(implicit connection: Connection) =
		{
			// Hashes the password
			val newHash = PasswordHash.createHash(newPassword)
			// Updates the password hash in the DB
			UserAuthModel.withUserId(a.id).withHash(newHash).update()
		}
	}
}
