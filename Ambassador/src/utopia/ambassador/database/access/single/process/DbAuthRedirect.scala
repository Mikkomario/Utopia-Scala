package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthRedirectFactory
import utopia.ambassador.database.model.process.AuthRedirectModel
import utopia.ambassador.model.stored.process.AuthRedirect
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual AuthRedirects
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthRedirect 
	extends SingleRowModelAccess[AuthRedirect] with NonDeprecatedView[AuthRedirect] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthRedirectModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthRedirectFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthRedirect instance
	  * @return An access point to that AuthRedirect
	  */
	def apply(id: Int) = DbSingleAuthRedirect(id)
	
	/**
	  * @param preparationId Id of the linked authentication preparation
	  * @return An access point to that redirection (even if expired)
	  */
	def forPreparationWithId(preparationId: Int) = new DbRedirectForPreparation(preparationId)
	
	
	// NESTED   --------------------
	
	class DbRedirectWithToken(token: String) extends UniqueAuthRedirectAccess with SubView
	{
		override protected def parent = DbAuthRedirect
		override protected def defaultOrdering = Some(factory.defaultOrdering)
		
		override def filterCondition = model.withToken(token).toCondition
	}
	
	class DbRedirectForPreparation(preparationId: Int) extends UniqueAuthRedirectAccess
	{
		override protected def defaultOrdering = Some(factory.defaultOrdering)
		override def globalCondition = Some(model.withPreparationId(preparationId).toCondition)
	}
}

