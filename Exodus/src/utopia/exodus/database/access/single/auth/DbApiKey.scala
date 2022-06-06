package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.ApiKeyFactory
import utopia.exodus.database.model.auth.ApiKeyModel
import utopia.exodus.model.stored.auth.ApiKey
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual ApiKeys
  * @author Mikko Hilpinen
  * @since 2021-10-25
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbApiKey extends SingleRowModelAccess[ApiKey] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = ApiKeyModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = ApiKeyFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted ApiKey instance
	  * @return An access point to that ApiKey
	  */
	def apply(id: Int) = DbSingleApiKey(id)
	
	/**
	  * @param token An api key token
	  * @return An access point to key with that token
	  */
	def apply(token: String) = new DbApiKeyByToken(token)
	
	
	// NESTED   --------------------
	
	class DbApiKeyByToken(token: String) extends UniqueApiKeyAccess with SubView
	{
		override protected def parent = DbApiKey
		
		override def filterCondition = model.withToken(token).toCondition
	}
}

