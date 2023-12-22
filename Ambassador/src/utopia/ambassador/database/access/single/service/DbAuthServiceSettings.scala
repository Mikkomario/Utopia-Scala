package utopia.ambassador.database.access.single.service

import utopia.ambassador.database.factory.service.AuthServiceSettingsFactory
import utopia.ambassador.database.model.service.AuthServiceSettingsModel
import utopia.ambassador.model.stored.service.AuthServiceSettings
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.LatestModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

/**
  * Used for accessing individual AuthServiceSettings
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthServiceSettings 
	extends SingleRowModelAccess[AuthServiceSettings] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthServiceSettingsModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthServiceSettingsFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthServiceSettings instance
	  * @return An access point to that AuthServiceSettings
	  */
	def apply(id: Int) = DbSingleAuthServiceSettings(id)
	
	/**
	  * @param serviceId Id of the targeted service
	  * @return An access point to that service's settings
	  */
	def forServiceWithId(serviceId: Int) = new DbSettingsForAuthService(serviceId)
	
	
	// NESTED   --------------------
	
	class DbSettingsForAuthService(serviceId: Int)
		extends UniqueAuthServiceSettingsAccess with LatestModelAccess[AuthServiceSettings] with SubView
	{
		override protected def parent = DbAuthServiceSettings
		override def filterCondition = this.model.withServiceId(serviceId).toCondition
	}
}

