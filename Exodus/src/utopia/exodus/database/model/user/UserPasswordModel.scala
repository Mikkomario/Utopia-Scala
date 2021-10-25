package utopia.exodus.database.model.user

import java.time.Instant
import utopia.exodus.database.factory.user.UserPasswordFactory
import utopia.exodus.model.partial.user.UserPasswordData
import utopia.exodus.model.stored.user.UserPassword
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

/**
  * Used for constructing UserPasswordModel instances and for inserting UserPasswords to the database
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
object UserPasswordModel extends DataInserter[UserPasswordModel, UserPassword, UserPasswordData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains UserPassword userId
	  */
	val userIdAttName = "userId"
	/**
	  * Name of the property that contains UserPassword hash
	  */
	val hashAttName = "hash"
	/**
	  * Name of the property that contains UserPassword created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains UserPassword userId
	  */
	def userIdColumn = table(userIdAttName)
	/**
	  * Column that contains UserPassword hash
	  */
	def hashColumn = table(hashAttName)
	/**
	  * Column that contains UserPassword created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = UserPasswordFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: UserPasswordData) = 
		apply(None, Some(data.userId), Some(data.hash), Some(data.created))
	
	override def complete(id: Value, data: UserPasswordData) = UserPassword(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this UserPassword was first created
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	/**
	  * @param hash User's hashed password
	  * @return A model containing only the specified hash
	  */
	def withHash(hash: String) = apply(hash = Some(hash))
	/**
	  * @param id A UserPassword id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	/**
	  * @param userId Id of the user who owns this password
	  * @return A model containing only the specified userId
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
}

/**
  * Used for interacting with UserPasswords in the database
  * @param id UserPassword database id
  * @param userId Id of the user who owns this password
  * @param hash User's hashed password
  * @param created Time when this UserPassword was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class UserPasswordModel(id: Option[Int] = None, userId: Option[Int] = None, hash: Option[String] = None, 
	created: Option[Instant] = None) 
	extends StorableWithFactory[UserPassword]
{
	// IMPLEMENTED	--------------------
	
	override def factory = UserPasswordModel.factory
	
	override def valueProperties = 
	{
		import UserPasswordModel._
		Vector("id" -> id, userIdAttName -> userId, hashAttName -> hash, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created A new created
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	/**
	  * @param hash A new hash
	  * @return A new copy of this model with the specified hash
	  */
	def withHash(hash: String) = copy(hash = Some(hash))
	/**
	  * @param userId A new userId
	  * @return A new copy of this model with the specified userId
	  */
	def withUserId(userId: Int) = copy(userId = Some(userId))
}

