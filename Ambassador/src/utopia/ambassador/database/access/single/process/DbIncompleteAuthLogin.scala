package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.database.model.process.IncompleteAuthLoginModel
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual IncompleteAuthLogins
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbIncompleteAuthLogin 
	extends SingleRowModelAccess[IncompleteAuthLogin] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthLoginModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthLoginFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted IncompleteAuthLogin instance
	  * @return An access point to that IncompleteAuthLogin
	  */
	def apply(id: Int) = DbSingleIncompleteAuthLogin(id)
}

