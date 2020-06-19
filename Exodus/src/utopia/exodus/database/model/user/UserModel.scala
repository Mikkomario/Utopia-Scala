package utopia.exodus.database.model.user

import utopia.exodus.database.factory.user.UserFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.User
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable

object UserModel
{
	// OTHER	-------------------------------------
	
	/**
	  * Inserts a new user to the database
	  * @param settings User settings
	  * @param password Password for this user (not hashed yet)
	  * @param connection DB Connection (implicit)
	  * @return Newly inserted user
	  */
	def insert(settings: UserSettingsData, password: String)(implicit connection: Connection) =
	{
		// Inserts the user first, then links new data
		val userId = apply().insert().getInt
		val newSettings = UserSettingsModel.insert(userId, settings)
		UserAuthModel.insert(userId, password)
		User(userId, newSettings)
	}
}

/**
  * Used for interacting with user data in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class UserModel(id: Option[Int] = None) extends Storable
{
	override def table = UserFactory.table
	
	override def valueProperties = Vector("id" -> id)
}
