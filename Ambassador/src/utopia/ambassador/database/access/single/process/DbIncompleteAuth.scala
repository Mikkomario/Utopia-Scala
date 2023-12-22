package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.IncompleteAuthFactory
import utopia.ambassador.database.model.process.IncompleteAuthModel
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual IncompleteAuths
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
object DbIncompleteAuth 
	extends SingleRowModelAccess[IncompleteAuth] with NonDeprecatedView[IncompleteAuth] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IncompleteAuthModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IncompleteAuthFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted IncompleteAuth instance
	  * @return An access point to that IncompleteAuth
	  */
	def apply(id: Int) = DbSingleIncompleteAuth(id)
	
	/**
	  * @param token An incomplete authentication token
	  * @return An access point to an authentication attempt with that token
	  */
	def withToken(token: String) = new DbIncompleteAuthWithToken(token)
	
	
	// NESTED   --------------------
	
	class DbIncompleteAuthWithToken(token: String) extends UniqueIncompleteAuthAccess with SubView
	{
		override protected def parent = DbIncompleteAuth
		
		override def filterCondition = this.model.withToken(token).toCondition
	}
}

