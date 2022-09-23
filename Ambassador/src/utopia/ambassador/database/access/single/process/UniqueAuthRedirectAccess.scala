package utopia.ambassador.database.access.single.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthRedirects.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthRedirectAccess 
	extends SingleRowModelAccess[AuthRedirect] 
		with DistinctModelAccess[AuthRedirect, Option[AuthRedirect], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the preparation event for this redirection. None if no instance (or value) was found.
	  */
	def preparationId(implicit connection: Connection) = pullColumn(model.preparationIdColumn).int
	
	/**
	  * The token of this instance. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	
	/**
	  * Time when the linked redirect token expires. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Time when this AuthRedirect was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthRedirectModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthRedirect instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the expires of the targeted AuthRedirect instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the preparationId of the targeted AuthRedirect instance(s)
	  * @param newPreparationId A new preparationId to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def preparationId_=(newPreparationId: Int)(implicit connection: Connection) = 
		putColumn(model.preparationIdColumn, newPreparationId)
	
	/**
	  * Updates the token of the targeted AuthRedirect instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any AuthRedirect instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
}

