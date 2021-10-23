package utopia.citadel.database.model.user

import utopia.citadel.database.Tables
import utopia.citadel.database.factory.user.UserLanguageFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserLanguageData
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

object UserLanguageModel extends DataInserter[UserLanguageModel, UserLanguage, UserLanguageData]
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
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(data: UserLanguageData) =
		apply(None, Some(data.userId), Some(data.languageId), Some(data.familiarityId))
	
	override protected def complete(id: Value, data: UserLanguageData) = UserLanguage(id.getInt, data)
	
	
	// OTHER	-----------------------------
	
	/**
	  * @param userId Id of the described user
	  * @return Model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with user-language-links in DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1.0
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
