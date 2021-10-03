package utopia.ambassador.database.model.process

import utopia.ambassador.database.factory.process.IncompleteAuthFactory
import utopia.ambassador.model.partial.process.IncompleteAuthData
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.vault.nosql.storable.deprecation.Expiring
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

object IncompleteAuthModel extends DataInserter[IncompleteAuthModel, IncompleteAuth, IncompleteAuthData]
	with Expiring
{
	// ATTRIBUTES   ------------------------
	
	override val deprecationAttName = "expiration"
	
	
	// COMPUTED ----------------------------
	
	/**
	  * @return The factory used by this model type
	  */
	def factory = IncompleteAuthFactory
	
	
	// IMPLEMENTED  ------------------------
	
	override def table = factory.table
	
	override def apply(data: IncompleteAuthData) =
		apply(None, Some(data.serviceId), Some(data.code), Some(data.token), Some(data.created),
			Some(data.expiration))
	
	override protected def complete(id: Value, data: IncompleteAuthData) = IncompleteAuth(id.getInt, data)
	
	
	// OTHER    ----------------------------
	
	/**
	  * @param token Incomplete authentication case token
	  * @return A model with that token
	  */
	def withToken(token: String) = apply(token = Some(token))
}

/**
  * Used for interacting with incomplete authentication attempts in the DB
  * @author Mikko Hilpinen
  * @since 18.7.2021, v1.0
  */
case class IncompleteAuthModel(id: Option[Int] = None, serviceId: Option[Int] = None, code: Option[String] = None,
                               token: Option[String] = None, created: Option[Instant] = None,
                               expiration: Option[Instant] = None) extends StorableWithFactory[IncompleteAuth]
{
	import IncompleteAuthModel._
	
	override def factory = IncompleteAuthModel.factory
	
	override def valueProperties = Vector("id" -> id, "serviceId" -> serviceId, "code" -> code,
		"token" -> token, "created" -> created, deprecationAttName -> expiration)
}
