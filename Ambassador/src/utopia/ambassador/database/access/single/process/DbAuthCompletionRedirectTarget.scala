package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.AuthCompletionRedirectTargetFactory
import utopia.ambassador.database.model.process.AuthCompletionRedirectTargetModel
import utopia.ambassador.model.stored.process.AuthCompletionRedirectTarget
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual AuthCompletionRedirectTargets
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbAuthCompletionRedirectTarget 
	extends SingleRowModelAccess[AuthCompletionRedirectTarget] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = AuthCompletionRedirectTargetModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthCompletionRedirectTargetFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted AuthCompletionRedirectTarget instance
	  * @return An access point to that AuthCompletionRedirectTarget
	  */
	def apply(id: Int) = DbSingleAuthCompletionRedirectTarget(id)
}

