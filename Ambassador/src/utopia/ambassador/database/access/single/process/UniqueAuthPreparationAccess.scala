package utopia.ambassador.database.access.single.process

import java.time.Instant
import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct AuthPreparations.
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait UniqueAuthPreparationAccess 
	extends SingleRowModelAccess[AuthPreparation] 
		with DistinctModelAccess[AuthPreparation, Option[AuthPreparation], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the user who initiated this process. None if no instance (or value) was found.
	  */
	def userId(implicit connection: Connection) = pullColumn(model.userIdColumn).int
	
	/**
	  * Token used for authenticating the OAuth redirect. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	
	/**
	  * Time when this authentication (token) expires. None if no instance (or value) was found.
	  */
	def expires(implicit connection: Connection) = pullColumn(model.expiresColumn).instant
	
	/**
	  * Time when this AuthPreparation was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.created).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted AuthPreparation instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.created, newCreated)
	
	/**
	  * Updates the expires of the targeted AuthPreparation instance(s)
	  * @param newExpires A new expires to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def expires_=(newExpires: Instant)(implicit connection: Connection) = 
		putColumn(model.expiresColumn, newExpires)
	
	/**
	  * Updates the token of the targeted AuthPreparation instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
	
	/**
	  * Updates the userId of the targeted AuthPreparation instance(s)
	  * @param newUserId A new userId to assign
	  * @return Whether any AuthPreparation instance was affected
	  */
	def userId_=(newUserId: Int)(implicit connection: Connection) = putColumn(model.userIdColumn, newUserId)
}

