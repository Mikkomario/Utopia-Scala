package utopia.exodus.model.partial.user

import utopia.flow.collection.value.typeless.Model

import java.time.Instant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.time.Now

/**
  * Represents a hashed user password
  * @param userId Id of the user who owns this password
  * @param hash User's hashed password
  * @param created Time when this UserPassword was first created
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
case class UserPasswordData(userId: Int, hash: String, created: Instant = Now) extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("user_id" -> userId, "hash" -> hash, "created" -> created))
}

