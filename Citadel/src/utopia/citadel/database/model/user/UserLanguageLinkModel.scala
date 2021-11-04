package utopia.citadel.database.model.user

import java.time.Instant
import utopia.citadel.database.factory.user.UserLanguageLinkFactory
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing UserLanguageModel instances and for inserting UserLanguages to the database
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserLanguageLinkModel extends DataInserter[UserLanguageLinkModel, UserLanguageLink, UserLanguageLinkData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains UserLanguage userId
	  */
	val userIdAttName = "userId"
	
	/**
	  * Name of the property that contains UserLanguage languageId
	  */
	val languageIdAttName = "languageId"
	
	/**
	  * Name of the property that contains UserLanguage familiarityId
	  */
	val familiarityIdAttName = "familiarityId"
	
	/**
	  * Name of the property that contains UserLanguage created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains UserLanguage userId
	  */
	def userIdColumn = table(userIdAttName)
	
	/**
	  * Column that contains UserLanguage languageId
	  */
	def languageIdColumn = table(languageIdAttName)
	
	/**
	  * Column that contains UserLanguage familiarityId
	  */
	def familiarityIdColumn = table(familiarityIdAttName)
	
	/**
	  * Column that contains UserLanguage created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = UserLanguageLinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: UserLanguageLinkData) =
		apply(None, Some(data.userId), Some(data.languageId), Some(data.familiarityId), Some(data.created))
	
	override def complete(id: Value, data: UserLanguageLinkData) = UserLanguageLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this UserLanguage was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param familiarityId Id of the user's familiarity level in the referenced language
	  * @return A model containing only the specified familiarityId
	  */
	def withFamiliarityId(familiarityId: Int) = apply(familiarityId = Some(familiarityId))
	
	/**
	  * @param id A UserLanguage id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param languageId Id of the language known to the user
	  * @return A model containing only the specified languageId
	  */
	def withLanguageId(languageId: Int) = apply(languageId = Some(languageId))
	
	/**
	  * @param userId Id of the user who's being described
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with UserLanguages in the database
  * @param id UserLanguage database id
  * @param userId Id of the user who's being described
  * @param languageId Id of the language known to the user
  * @param familiarityId Id of the user's familiarity level in the referenced language
  * @param created Time when this UserLanguage was first created
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserLanguageLinkModel(id: Option[Int] = None, userId: Option[Int] = None,
                                 languageId: Option[Int] = None, familiarityId: Option[Int] = None,
                                 created: Option[Instant] = None)
	extends StorableWithFactory[UserLanguageLink]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageLinkModel.factory
	
	override def valueProperties =
	{
		import UserLanguageLinkModel._
		Vector("id" -> id, userIdAttName -> userId, languageIdAttName -> languageId, 
			familiarityIdAttName -> familiarityId, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param familiarityId A new familiarityId
	  * @return A new copy of this model with the specified familiarityId
	  */
	def withFamiliarityId(familiarityId: Int) = copy(familiarityId = Some(familiarityId))
	
	/**
	  * @param languageId A new languageId
	  * @return A new copy of this model with the specified languageId
	  */
	def withLanguageId(languageId: Int) = copy(languageId = Some(languageId))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}

