package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.model.partial.process.AuthRedirectData
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.citadel.database.model.Expiring
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant

object AuthRedirectModel extends DataInserter[AuthRedirectModel, AuthRedirect, AuthRedirectData]
	with Expiring
{
	// ATTRIBUTES   ---------------------
	
	override val deprecationAttName = "expiration"
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Factory used by this model type
	  */
	def factory = AuthRedirectFactory
	
	
	// IMPLEMENTED  ---------------------
	
	override def table = factory.table
	
	override def apply(data: AuthRedirectData) =
		apply(None, Some(data.preparationId), Some(data.token), Some(data.created), Some(data.expiration))
	
	override protected def complete(id: Value, data: AuthRedirectData) = AuthRedirect(id.getInt, data)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param preparationId An authentication preparation id
	  * @return A model with that preparation id
	  */
	def withPreparationId(preparationId: Int) = apply(preparationId = Some(preparationId))
}

/**
  * Used for interacting with authentication user redirects in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthRedirectModel(id: Option[Int] = None, preparationId: Option[Int] = None,
                             token: Option[String] = None, created: Option[Instant] = None,
                             expiration: Option[Instant] = None)
	extends StorableWithFactory[AuthRedirect]
{
	import AuthRedirectModel._
	
	override def factory = AuthRedirectModel.factory
	
	override def valueProperties = Vector("id" -> id, "preparationId" -> preparationId, "token" -> token,
		"created" -> created, deprecationAttName -> expiration)
}
