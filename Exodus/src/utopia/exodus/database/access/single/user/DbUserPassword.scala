package utopia.exodus.database.access.single.user

import utopia.exodus.database.factory.user.UserPasswordFactory
import utopia.exodus.database.model.user.UserPasswordModel
import utopia.exodus.model.partial.user.UserPasswordData
import utopia.exodus.model.stored.user.UserPassword
import utopia.exodus.util.PasswordHash
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual UserPasswords
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object DbUserPassword extends SingleRowModelAccess[UserPassword] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserPasswordModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserPasswordFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserPassword instance
	  * @return An access point to that UserPassword
	  */
	def apply(id: Int) = DbSingleUserPassword(id)
	
	/**
	  * @param userId Id of targeted user
	  * @return An access point to that user's password
	  */
	def ofUserWithId(userId: Int) = new DbPasswordOfUser(userId)
	
	
	// NESTED   ---------------------
	
	class DbPasswordOfUser(userId: Int) extends UniqueUserPasswordAccess with SubView
	{
		// IMPLEMENTED  -------------
		
		override protected def parent = DbUserPassword
		override protected def defaultOrdering = None
		
		override def filterCondition = model.withUserId(userId).toCondition
		
		
		// OTHER    -----------------
		
		/**
		  * Tests the specified password against
		  * @param password Proposed user password
		  * @param connection Implicit DB Connection
		  * @return Whether that password is the correct password for this user
		  */
		def test(password: String)(implicit connection: Connection) =
			hash.exists { PasswordHash.validatePassword(password, _) }
		
		/**
		  * Updates this password
		  * @param newPassword New password to assign (will be hashed)
		  * @param connection Implicit DB Connection
		  * @return Newly inserted user password instance
		  */
		def update(newPassword: String)(implicit connection: Connection) =
		{
			delete()
			model.insert(UserPasswordData(userId, PasswordHash.createHash(newPassword)))
		}
	}
}

