package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.database.model.process.IncompleteAuthLoginModel
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{SubView, UnconditionalView}

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
	
	/**
	  * @param authId An incomplete authentication id
	  * @return An access point to the login completion for that authentication attempt
	  */
	def forAuthenticationWithId(authId: Int) = new DbLoginForIncompleteAuth(authId)
	
	
	// NESTED   ---------------------
	
	class DbLoginForIncompleteAuth(incompleteAuthId: Int) extends UniqueIncompleteAuthLoginAccess with SubView
	{
		override protected def parent = DbIncompleteAuthLogin
		
		override def filterCondition = this.model.withAuthId(incompleteAuthId).toCondition
	}
}

