package utopia.ambassador.database.access.single.service

import utopia.ambassador.database.factory.service.AuthServiceFactory
import utopia.ambassador.database.model.service.AuthServiceModel
import utopia.ambassador.model.stored.service.AuthService
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual AuthServices
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthService extends SingleRowModelAccess[AuthService] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthServiceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthService instance
	  * @return An access point to that AuthService
	  */
	def apply(id: Int) = DbSingleAuthService(id)
	
	/**
	  * @param serviceName Name of the targeted service
	  * @return An access point to that service
	  */
	def withName(serviceName: String) = new DbAuthServiceWithName(serviceName)
	
	
	// NESTED   --------------------
	
	class DbAuthServiceWithName(name: String) extends UniqueAuthServiceAccess with SubView
	{
		override protected def parent = DbAuthService
		
		override def filterCondition = model.withName(name).toCondition
	}
}

