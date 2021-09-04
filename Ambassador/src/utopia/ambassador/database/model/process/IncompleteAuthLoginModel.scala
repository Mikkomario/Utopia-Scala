package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.model.partial.process.IncompleteAuthLoginData
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant

object IncompleteAuthLoginModel
	extends DataInserter[IncompleteAuthLoginModel, IncompleteAuthLogin, IncompleteAuthLoginData]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return The factory used by this model type
	  */
	def factory = IncompleteAuthLoginFactory
	
	
	// IMPLEMENTED  --------------------------
	
	override def table = factory.table
	
	override def apply(data: IncompleteAuthLoginData) =
		apply(None, Some(data.authenticationId), Some(data.userId), Some(data.wasSuccess), Some(data.created))
	
	override protected def complete(id: Value, data: IncompleteAuthLoginData) = IncompleteAuthLogin(id.getInt, data)
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param authenticationId Id of the incomplete authentication this login closes
	  * @return A model with that authentication id
	  */
	def withAuthenticationId(authenticationId: Int) =
		apply(authenticationId = Some(authenticationId))
}

/**
  * Used for interacting with incomplete authentication closures in DB
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
case class IncompleteAuthLoginModel(id: Option[Int] = None, authenticationId: Option[Int] = None,
                                    userId: Option[Int] = None, wasSuccess: Option[Boolean] = None,
                                    created: Option[Instant] = None)
	extends StorableWithFactory[IncompleteAuthLogin]
{
	override def factory = IncompleteAuthLoginModel.factory
	
	override def valueProperties = Vector("id" -> id, "authenticationId" -> authenticationId, "userId" -> userId,
		"wasSuccess" -> wasSuccess, "created" -> created)
}