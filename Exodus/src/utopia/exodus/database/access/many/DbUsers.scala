package utopia.exodus.database.access.many

import utopia.exodus.database.access.single.{DbDevice, DbLanguage}
import utopia.exodus.database.factory.user.{UserFactory, UserSettingsFactory}
import utopia.exodus.database.model.user.{UserDeviceModel, UserLanguageModel, UserModel, UserSettingsModel}
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.user.UserWithLinks
import utopia.metropolis.model.error.{AlreadyUsedException, IllegalPostModelException}
import utopia.metropolis.model.partial.user.{UserLanguageData, UserSettingsData}
import utopia.metropolis.model.post.NewUser
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.ManyModelAccess
import utopia.vault.sql.SqlExtensions._

import scala.util.{Failure, Success, Try}

/**
  * Used for accessing multiple user's data at once
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object DbUsers extends ManyModelAccess[User]
{
	// IMPLEMENTED	--------------------------
	
	override def factory = UserFactory
	
	override def globalCondition = Some(factory.nonDeprecatedCondition)
	
	override protected def defaultOrdering = None
	
	
	// COMPUTED	------------------------------
	
	private def settingsModel = UserSettingsModel
	
	private def settingsFactory = UserSettingsFactory
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param userIds Targeted user ids
	  * @return An access point to those users' data
	  */
	def apply(userIds: Set[Int]) = new DbUsersSubgroup(userIds)
	
	/**
	  * Checks whether a user name is currently in use
	  * @param userName Tested user name
	  * @param connection DB Connection (implicit)
	  * @return Whether specified user name is currently in use
	  */
	def existsUserWithName(userName: String)(implicit connection: Connection) =
	{
		settingsFactory.exists(settingsModel.withName(userName).toCondition && settingsFactory.nonDeprecatedCondition)
	}
	
	/**
	  * Checks whether a user email is currently in use
	  * @param email Tested user email
	  * @param connection DB Connection (implicit)
	  * @return Whether specified email address is currently in use
	  */
	def existsUserWithEmail(email: String)(implicit connection: Connection) =
	{
		settingsFactory.exists(settingsModel.withEmail(email).toCondition && settingsFactory.nonDeprecatedCondition)
	}
	
	/**
	  * Inserts a new user to the DB
	  * @param newUser New user data to insert
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted data. Failure with IllegalPostModelException if posted data was invalid. Failure with
	  *         AlreadyUsedException if user name or email was already in use.
	  */
	def tryInsert(newUser: NewUser, email: String)(implicit connection: Connection): Try[UserWithLinks] =
	{
		// Checks whether the proposed email already exist
		val userName = newUser.userName.trim
		
		if (!email.contains('@'))
			Failure(new IllegalPostModelException("Email must be a valid email address"))
		else if (userName.isEmpty)
			Failure(new IllegalPostModelException("User name must not be empty"))
		else if (existsUserWithEmail(email))
			Failure(new AlreadyUsedException("Email is already in use"))
		else
		{
			// Makes sure provided device id or language id matches data in the DB
			val idsAreValid = newUser.device.forall
			{
				case Right(deviceId) => DbDevice(deviceId).isDefined
				case Left(newDevice) => DbLanguage(newDevice.languageId).isDefined
			}
			
			if (idsAreValid)
			{
				// Makes sure all the specified languages are also valid
				DbLanguage.validateProposedProficiencies(newUser.languages).flatMap { languages =>
					// Inserts new user data
					val user = UserModel.insert(UserSettingsData(userName, email), newUser.password)
					val insertedLanguages = languages.map { case (languageId, familiarity) =>
						UserLanguageModel.insert(UserLanguageData(user.id, languageId, familiarity)) }
					// Links user with device (if device has been specified) (uses existing or a new device)
					val deviceId = newUser.device.map
					{
						case Right(deviceId) => deviceId
						case Left(newDevice) => DbDevices.insert(newDevice.name, newDevice.languageId, user.id).targetId
					}
					deviceId.foreach { UserDeviceModel.insert(user.id, _) }
					// Returns inserted user
					Success(UserWithLinks(user, insertedLanguages, deviceId.toVector))
				}
			}
			else
				Failure(new IllegalPostModelException("device_id and language_id must point to existing data"))
		}
	}
	
	
	// NESTED	--------------------------------
	
	class DbUsersSubgroup(userIds: Set[Int]) extends ManyModelAccess[User]
	{
		// IMPLEMENTED	------------------------
		
		override def factory = DbUsers.factory
		
		override def globalCondition =
			Some(DbUsers.mergeCondition(factory.table.primaryColumn.get.in(userIds)))
		
		override protected def defaultOrdering = None
		
		
		// COMPUTED	----------------------------
		
		/**
		  * @param connection Database connection (implicit)
		  * @return Current settings for users with these ids
		  */
		def settings(implicit connection: Connection) = settingsFactory.getMany(
			settingsFactory.nonDeprecatedCondition && settingsModel.userIdColumn.in(userIds))
	}
}
