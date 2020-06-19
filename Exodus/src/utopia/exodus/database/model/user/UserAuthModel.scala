package utopia.exodus.database.model.user

import utopia.exodus.database.Tables
import utopia.exodus.util.PasswordHash
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Storable

object UserAuthModel
{
	// ATTRIBUTES	---------------------------
	
	/**
	  * Name of the attribute that contains password hash
	  */
	val hashAttName = "hash"
	
	
	// COMPUTED	-------------------------------
	
	def table = Tables.userAuth
	
	
	// OTHER	-------------------------------
	
	/**
	  * @param userId Id of targeted user
	  * @return A model with only user id set
	  */
	def withUserId(userId: Int) = apply(userId = Some(userId))
	
	/**
	  * Inserts a new password to the DB
	  * @param userId Id of the user for which the password is inserted
	  * @param password Password for the user (not hashed)
	  * @param connection DB Connection (implicit)
	  */
	def insert(userId: Int, password: String)(implicit connection: Connection): Unit =
	{
		// Hashes the password, then inserts it to DB
		val hash = PasswordHash.createHash(password)
		apply(None, Some(userId), Some(hash)).insert()
	}
}

/**
  * Used for interacting with user passwords in the DB
  * @author Mikko Hilpinen
  * @since 2.5.2020, v1
  */
case class UserAuthModel(id: Option[Int] = None, userId: Option[Int] = None, hash: Option[String] = None) extends Storable
{
	override def table = UserAuthModel.table
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, UserAuthModel.hashAttName -> hash)
}
