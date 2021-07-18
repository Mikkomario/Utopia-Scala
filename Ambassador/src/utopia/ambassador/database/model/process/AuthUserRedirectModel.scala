package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.AuthUserRedirectFactory
import utopia.ambassador.model.partial.process.AuthUserRedirectData
import utopia.ambassador.model.stored.process.AuthUserRedirect
import utopia.citadel.database.model.Expiring
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.DataInserter

import java.time.Instant

object AuthUserRedirectModel extends DataInserter[AuthUserRedirectModel, AuthUserRedirect, AuthUserRedirectData]
	with Expiring
{
	// ATTRIBUTES   ---------------------
	
	override val deprecationAttName = "expiration"
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return Factory used by this model type
	  */
	def factory = AuthUserRedirectFactory
	
	override def table = factory.table
	
	override def apply(data: AuthUserRedirectData) =
		apply(None, Some(data.preparationId), Some(data.token), Some(data.created), Some(data.expiration))
	
	override protected def complete(id: Value, data: AuthUserRedirectData) = AuthUserRedirect(id.getInt, data)
}

/**
  * Used for interacting with authentication user redirects in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class AuthUserRedirectModel(id: Option[Int] = None, preparationId: Option[Int] = None,
                                 token: Option[String] = None, created: Option[Instant] = None,
                                 expiration: Option[Instant] = None)
	extends StorableWithFactory[AuthUserRedirect]
{
	import AuthUserRedirectModel._
	
	override def factory = AuthUserRedirectModel.factory
	
	override def valueProperties = Vector("id" -> id, "preparationId" -> preparationId, "token" -> token,
		"created" -> created, deprecationAttName -> expiration)
}
