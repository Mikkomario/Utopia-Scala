package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthPreparationScopeLinkFactory
import utopia.ambassador.database.model.process.AuthPreparationScopeLinkModel
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual AuthPreparationScopeLinks
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthPreparationScopeLink 
	extends SingleRowModelAccess[AuthPreparationScopeLink] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthPreparationScopeLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationScopeLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthPreparationScopeLink instance
	  * @return An access point to that AuthPreparationScopeLink
	  */
	def apply(id: Int) = DbSingleAuthPreparationScopeLink(id)
}

