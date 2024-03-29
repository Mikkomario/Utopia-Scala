package utopia.ambassador.database.access.single.service

import utopia.ambassador.database.access.many.scope.DbScopes
import utopia.ambassador.database.factory.service.{AuthServiceFactory, ServiceSettingsFactory}
import utopia.ambassador.database.model.service.ServiceSettingsModel
import utopia.ambassador.model.stored.service.{AuthService, ServiceSettings}
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.{LatestModelAccess, SingleIntIdModelAccess}
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for reading data concerning individual 3rd party services
  * @author Mikko Hilpinen
  * @since 15.7.2021, v1.0
  */
object DbAuthService extends SingleRowModelAccess[AuthService] with UnconditionalView
{
	// IMPLEMENTED  ---------------------------
	
	override def factory = AuthServiceFactory
	
	
	// OTHER    -------------------------------
	
	/**
	  * @param serviceId A service id
	  * @return An access point to that service's data
	  */
	def apply(serviceId: Int) = new DbSingleAuthService(serviceId)
	
	
	// NESTED   -------------------------------
	
	class DbSingleAuthService(override val id: Int) extends SingleIntIdModelAccess[AuthService]
	{
		// COMPUTED ---------------------------
		
		/**
		  * @return An access point to this service's settings
		  */
		def settings = DbServiceSettings
		/**
		  * @return An access point to this service's scopes
		  */
		def scopes = DbScopes.forServiceWithId(id)
		/**
		  * @param connection Implicit DB connection
		  * @return Ids of the tasks that require authentication with this service
		  */
		def taskIds(implicit connection: Connection) = scopes.taskIds
		
		
		// IMPLEMENTED  -----------------------
		
		override def factory = DbAuthService.factory
		
		
		// NESTED   ---------------------------
		
		object DbServiceSettings extends LatestModelAccess[ServiceSettings]
		{
			// COMPUTED -----------------------
			
			private def model = ServiceSettingsModel
			
			/**
			  * @param connection Implicit DB Connection
			  * @return The default client side redirect url for this service for authentication completions
			  */
			def defaultCompletionUrl(implicit connection: Connection) =
				pullAttribute(model.defaultCompletionUrlAttName).string
			
			
			// IMPLEMENTED  -------------------
			
			override def factory = ServiceSettingsFactory
			
			override def globalCondition = Some(model.withServiceId(id).toCondition)
		}
	}
}
