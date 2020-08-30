package utopia.exodus.database.model.user

import utopia.exodus.database.Tables
import utopia.exodus.database.factory.user.UserLanguageFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserLanguageData
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.database.Connection
import utopia.vault.model.immutable.StorableWithFactory

object UserLanguageModel
{
	// ATTRIBUTES	-------------------------
	
	/**
	  * Name of the attribute that contains linked language's id
	  */
	val languageIdAttName = "languageId"
	
	
	// COMPUTED	-----------------------------
	
	/**
	  * @return Table used by this model
	  */
	def table = Tables.userLanguage
	
	/**
	  * @return Column that contains the associated language's id
	  */
	def languageIdColumn = table(languageIdAttName)
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param userId Id of the described user
	  * @return Model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * Inserts a new connection between a user and a language
	  * @param data New user language link to insert
	  * @return Id of the newly inserted link
	  */
	def insert(data: UserLanguageData)(implicit connection: Connection) =
	{
		val newId = apply(None, Some(data.userId), Some(data.languageId), Some(data.familiarityId)).insert().getInt
		UserLanguage(newId, data)
	}
}

/**
  * Used for interacting with user-language-links in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class UserLanguageModel(id: Option[Int] = None, userId: Option[Int] = None, languageId: Option[Int] = None,
							 familiarityId: Option[Int] = None)
	extends StorableWithFactory[UserLanguage]
{
	import UserLanguageModel._
	
	override def factory = UserLanguageFactory
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, languageIdAttName -> languageId,
		"familiarityId" -> familiarityId)
}
