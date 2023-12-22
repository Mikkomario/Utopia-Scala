package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthPreparationFactory
import utopia.ambassador.database.model.process.AuthPreparationModel
import utopia.ambassador.model.stored.process.AuthPreparation
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual AuthPreparations
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthPreparation 
	extends SingleRowModelAccess[AuthPreparation] with NonDeprecatedView[AuthPreparation] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthPreparation instance
	  * @return An access point to that AuthPreparation
	  */
	def apply(id: Int) = DbSingleAuthPreparation(id)
	
	/**
	  * @param token An auth preparation token
	  * @return An access point to that auth preparation
	  */
	def withToken(token: String) = new DbAuthPreparationWithToken(token)
	
	
	// NESTED   --------------------
	
	class DbAuthPreparationWithToken(token: String) extends UniqueAuthPreparationAccess with SubView
	{
		override protected def parent = DbAuthPreparation
		
		override def filterCondition = this.model.withToken(token).toCondition
	}
}

