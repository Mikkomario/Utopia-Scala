package utopia.ambassador.database.access.single.process

import java.time.Instant
import utopia.ambassador.database.factory.process.IncompleteAuthFactory
import utopia.ambassador.database.model.process.IncompleteAuthModel
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.flow.datastructure.immutable.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct IncompleteAuths.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueIncompleteAuthAccess 
	extends SingleRowModelAccess[IncompleteAuth] 
		with DistinctModelAccess[IncompleteAuth, Option[IncompleteAuth], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the service from which the user arrived. None if no instance (or value) was found.
	  */
	def serviceId(implicit connection: Connection) = pullColumn(model.serviceIdColumn).int
	
	/**
	  * Authentication code provided by the 3rd party service. None if no instance (or value) was found.
	  */
	def code(implicit connection: Connection) = pullColumn(model.codeColumn).string
	
	/**
	  * Token used for authentication the completion of this authentication. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	
	/**
	  * Time after which the generated authentication token is no longer valid. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Time when this IncompleteAuth was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the code of the targeted IncompleteAuth instance(s)
	  * @param newCode A new code to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def code_=(newCode: String)(implicit connection: Connection) = putColumn(model.codeColumn, newCode)
	
	/**
	  * Updates the created of the targeted IncompleteAuth instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the expires of the targeted IncompleteAuth instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the serviceId of the targeted IncompleteAuth instance(s)
	  * @param newServiceId A new serviceId to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def serviceId_=(newServiceId: Int)(implicit connection: Connection) = 
		putColumn(model.serviceIdColumn, newServiceId)
	
	/**
	  * Updates the token of the targeted IncompleteAuth instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any IncompleteAuth instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
}

