package utopia.ambassador.database.model.process

import java.time.Instant
import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.model.partial.process.IncompleteAuthLoginData
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing IncompleteAuthLoginModel instances and for inserting IncompleteAuthLogins to the database
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object IncompleteAuthLoginModel 
	extends DataInserter[IncompleteAuthLoginModel, IncompleteAuthLogin, IncompleteAuthLoginData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains IncompleteAuthLogin authId
	  */
	val authIdAttName = "authId"
	
	/**
	  * Name of the property that contains IncompleteAuthLogin userId
	  */
	val userIdAttName = "userId"
	
	/**
	  * Name of the property that contains IncompleteAuthLogin created
	  */
	val createdAttName = "created"
	
	/**
	  * Name of the property that contains IncompleteAuthLogin wasSuccess
	  */
	val wasSuccessAttName = "wasSuccess"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains IncompleteAuthLogin authId
	  */
	def authIdColumn = table(authIdAttName)
	
	/**
	  * Column that contains IncompleteAuthLogin userId
	  */
	def userIdColumn = table(userIdAttName)
	
	/**
	  * Column that contains IncompleteAuthLogin created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * Column that contains IncompleteAuthLogin wasSuccess
	  */
	def wasSuccessColumn = table(wasSuccessAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = IncompleteAuthLoginFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: IncompleteAuthLoginData) = 
		apply(None, Some(data.authId), Some(data.userId), Some(data.created), Some(data.wasSuccess))
	
	override def complete(id: Value, data: IncompleteAuthLoginData) = IncompleteAuthLogin(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param authId Id of the incomplete authentication this login completes
	  * @return A model containing only the specified authId
	  */
	def withAuthId(authId: Int) = apply(authId = Some(authId))
	
	/**
	  * @param created Time when this IncompleteAuthLogin was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A IncompleteAuthLogin id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param userId Id of the user who logged in
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * @param wasSuccess Whether authentication tokens were successfully acquired from the 3rd party service
	  * @return A model containing only the specified wasSuccess
	  */
	def withWasSuccess(wasSuccess: Boolean) = apply(wasSuccess = Some(wasSuccess))
}

/**
  * Used for interacting with IncompleteAuthLogins in the database
  * @param id IncompleteAuthLogin database id
  * @param authId Id of the incomplete authentication this login completes
  * @param userId Id of the user who logged in
  * @param created Time when this IncompleteAuthLogin was first created
  * @param wasSuccess Whether authentication tokens were successfully acquired from the 3rd party service
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class IncompleteAuthLoginModel(id: Option[Int] = None, authId: Option[Int] = None, 
	userId: Option[Int] = None, created: Option[Instant] = None, wasSuccess: Option[Boolean] = None) 
	extends StorableWithFactory[IncompleteAuthLogin]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthLoginModel.factory
	
	override def valueProperties = 
	{
		import IncompleteAuthLoginModel._
		Vector("id" -> id, authIdAttName -> authId, userIdAttName -> userId, createdAttName -> created, 
			wasSuccessAttName -> wasSuccess)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param authId A new authId
	  * @return A new copy of this model with the specified authId
	  */
	def withAuthId(authId: Int) = copy(authId = Some(authId))
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
	
	/**
	  * @param wasSuccess A new wasSuccess
	  * @return A new copy of this model with the specified wasSuccess
	  */
	def withWasSuccess(wasSuccess: Boolean) = copy(wasSuccess = Some(wasSuccess))
}

