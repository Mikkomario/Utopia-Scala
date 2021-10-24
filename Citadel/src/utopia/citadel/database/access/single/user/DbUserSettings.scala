package utopia.citadel.database.access.single.user

import utopia.citadel.database.access.many.user.DbManyUserSettings
import utopia.citadel.database.factory.user.UserSettingsFactory
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.metropolis.model.error.AlreadyUsedException
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

import scala.util.{Failure, Success}

/**
  * Used for accessing individual UserSettings
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserSettings 
	extends SingleRowModelAccess[UserSettings] with NonDeprecatedView[UserSettings] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserSettingsFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserSettings instance
	  * @return An access point to that UserSettings
	  */
	def apply(id: Int) = DbSingleUserSettings(id)
	
	/**
	  * @param userId Id of the targeted user
	  * @return An access point to that user's current settings
	  */
	def forUserWithId(userId: Int) = new DbSettingsForUser(userId)
	/**
	  * @param email An email address
	  * @return An access point to currently valid settings that use that email address
	  */
	def withEmail(email: String) = new DbSettingsWithEmail(email)
	
	
	// NESTED   ---------------------
	
	class DbSettingsForUser(userId: Int) extends UniqueUserSettingsAccess with SubView
	{
		// IMPLEMENTED  -------------
		
		override protected def parent = DbUserSettings
		override protected def defaultOrdering = None
		
		override def filterCondition = model.withUserId(userId).toCondition
		
		
		// OTHER    -----------------
		
		/**
		  * Attempts to update these settings, making sure email address (and possibly user name) don't overlap
		  * with existing options
		  * @param newName New user name to assign
		  * @param newEmail New email address to assign (optional, if None, there will be no email address)
		  * @param requireUniqueUserName Whether the specified user name must always be unique to this user
		  *                              (default = false = name must only be unique if no email address is provided)
		  * @param connection Implicit DB Connection
		  * @return New version of these settings. Failure if user name or email address were not unique.
		  */
		def tryUpdate(newName: String, newEmail: Option[String] = None, requireUniqueUserName: Boolean = false)
		             (implicit connection: Connection) =
		{
			def _replace() =
			{
				// Deprecates the old settings
				deprecate()
				// Inserts new settings
				model.insert(UserSettingsData(userId, newName, newEmail))
			}
			def _userNameIsValid() = DbManyUserSettings.withName(newName).userIds.forall { _ == userId }
			
			newEmail match {
				// Case: User has specified an email address => it will have to be unique
				case Some(email) =>
					// Makes sure the email address is still available (or belongs to this user)
					// May also check the user name
					if (DbUserSettings.withEmail(email).userId.forall { _ == userId } &&
						(!requireUniqueUserName || _userNameIsValid()))
						Success(_replace())
					else
						Failure(new AlreadyUsedException(s"Email address $email is already in use by another user"))
				// Case: User hasn't specified an email address => the user name must be unique
				case None =>
					if (_userNameIsValid())
						Success(_replace())
					else
						Failure(new AlreadyUsedException(
							s"User name $newName is already in use by another user"))
			}
		}
	}
	
	class DbSettingsWithEmail(email: String) extends UniqueUserSettingsAccess with SubView
	{
		// IMPLEMENTED  ----------------------------
		
		override protected def parent = DbUserSettings
		override protected def defaultOrdering = None
		
		override def filterCondition = model.withEmail(email).toCondition
	}
}

