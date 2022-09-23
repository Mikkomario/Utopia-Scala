package utopia.exodus.database.access.single.auth

import java.time.Instant
import utopia.exodus.database.factory.auth.ApiKeyFactory
import utopia.exodus.database.model.auth.ApiKeyModel
import utopia.exodus.model.stored.auth.ApiKey
import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed

/**
  * A common trait for access points that return individual and distinct ApiKeys.
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
trait UniqueApiKeyAccess 
	extends SingleRowModelAccess[ApiKey] with DistinctModelAccess[ApiKey, Option[ApiKey], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * The textual representation of this api key. None if no instance (or value) was found.
	  */
	def token(implicit connection: Connection) = pullColumn(model.tokenColumn).string
	
	/**
	  * Name given to identify this api key. None if no instance (or value) was found.
	  */
	def name(implicit connection: Connection) = pullColumn(model.nameColumn).string
	
	/**
	  * Time when this ApiKey was first created. None if no instance (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ApiKeyModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ApiKeyFactory
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the created of the targeted ApiKey instance(s)
	  * @param newCreated A new created to assign
	  * @return Whether any ApiKey instance was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the name of the targeted ApiKey instance(s)
	  * @param newName A new name to assign
	  * @return Whether any ApiKey instance was affected
	  */
	def name_=(newName: String)(implicit connection: Connection) = putColumn(model.nameColumn, newName)
	
	/**
	  * Updates the token of the targeted ApiKey instance(s)
	  * @param newToken A new token to assign
	  * @return Whether any ApiKey instance was affected
	  */
	def token_=(newToken: String)(implicit connection: Connection) = putColumn(model.tokenColumn, newToken)
}

