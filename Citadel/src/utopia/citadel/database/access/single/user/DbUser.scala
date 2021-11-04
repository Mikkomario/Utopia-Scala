package utopia.citadel.database.access.single.user

import utopia.citadel.database.access.single.device.DbClientDevice
import utopia.citadel.database.factory.user.UserFactory
import utopia.citadel.database.model.device.ClientDeviceUserModel
import utopia.citadel.database.model.user.{UserLanguageLinkModel, UserModel, UserSettingsModel}
import utopia.metropolis.model.combined.user.UserWithLinks
import utopia.metropolis.model.partial.device.ClientDeviceUserData
import utopia.metropolis.model.partial.user.{UserLanguageLinkData, UserSettingsData}
import utopia.metropolis.model.post.NewDevice
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual Users
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUser extends SingleRowModelAccess[User] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserModel
	
	private def settingsModel = UserSettingsModel
	private def languageLinkModel = UserLanguageLinkModel
	private def deviceLinkModel = ClientDeviceUserModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted User instance
	  * @return An access point to that User
	  */
	def apply(id: Int) = DbSingleUser(id)
	
	/**
	  * Inserts a new user to the database. Also inserts user settings, language links and a possible device connection
	  * @param name Name of this user
	  * @param email Email address of this user (optional)
	  * @param languageFamiliarities A map containing language id -> language familiarity id -links to assign for this
	  *                              user.
	  * @param device Either Some(Left): New device info or Some(Right): id of linked device or None.
	  *               Please make sure the referred ids are valid.
	  * @param connection Implicit DB Connection
	  * @return Inserted user data
	  */
	def insert(name: String, email: Option[String] = None, languageFamiliarities: Map[Int, Int] = Map(),
	           device: Option[Either[NewDevice, Int]] = None)
	          (implicit connection: Connection) =
	{
		// Inserts new user data
		val user = model.insert()
		val settings = settingsModel.insert(UserSettingsData(user.id, name, email))
		val languageLinks = languageLinkModel.insert(
			languageFamiliarities.toVector.sortBy { _._1 }
				.map { case (languageId, familiarityId) => UserLanguageLinkData(user.id, languageId, familiarityId) })
		// Links user with device (if device has been specified) (uses existing or a new device)
		val deviceId = device.map {
			case Right(deviceId) => deviceId
			case Left(newDevice) => DbClientDevice.insert(newDevice, user.id).id
		}
		deviceId.foreach { deviceId => deviceLinkModel.insert(ClientDeviceUserData(deviceId, user.id)) }
		// Returns inserted user with links included
		UserWithLinks(settings, languageLinks, deviceId.toVector)
	}
}

