package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.model.partial.process.AuthPreparationData
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.citadel.database.model.Expiring
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant

object AuthPreparationModel extends DataInserter[AuthPreparationModel, AuthPreparation, AuthPreparationData]
	with Expiring
{
	// ATTRIBUTES   ----------------------------
	
	override val deprecationAttName = "expiration"
	
	
	// COMPUTED --------------------------------
	
	/**
	  * @return The factory used by this model type
	  */
	def factory = AuthPreparationFactory
	
	
	// IMPLEMENTED  ----------------------------
	
	override def table = factory.table
	
	override def apply(data: AuthPreparationData) =
		apply(None, Some(data.userId), Some(data.token), data.clientState, Some(data.created), Some(data.expiration))
	
	override protected def complete(id: Value, data: AuthPreparationData) = AuthPreparation(id.getInt, data)
}

/**
  * Used for interacting with authentication preparations in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthPreparationModel(id: Option[Int] = None, userId: Option[Int] = None, token: Option[String] = None,
                                clientState: Option[String] = None, created: Option[Instant] = None,
                                expiration: Option[Instant] = None)
	extends StorableWithFactory[AuthPreparation]
{
	import AuthPreparationModel._
	
	override def factory = AuthPreparationModel.factory
	
	override def valueProperties = Vector("id" -> id, "userId" -> userId, "token" -> token,
		"clientState" -> clientState, "created" -> created, deprecationAttName -> expiration)
}
