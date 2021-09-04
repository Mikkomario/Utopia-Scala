package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.IncompleteAuthLoginFactory
import utopia.ambassador.database.model.process.IncompleteAuthLoginModel
import utopia.ambassador.model.stored.process.IncompleteAuthLogin
import utopia.flow.generic.ValueConversions._
import utopia.vault.nosql.access.single.model.SingleModelAccessById
import utopia.vault.nosql.access.single.model.distinct.UniqueModelAccess
import utopia.vault.nosql.view.RowFactoryView

/**
  * Used for accessing individual incomplete authentication logins at a time
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object DbIncompleteAuthLogin extends SingleModelAccessById[IncompleteAuthLogin, Int]
	with RowFactoryView[IncompleteAuthLogin]
{
	// COMPUTED ----------------------------------
	
	private def model = IncompleteAuthLoginModel
	
	
	// IMPLEMENTED  ------------------------------
	
	override def factory = IncompleteAuthLoginFactory
	
	override def idToValue(id: Int) = id
	
	
	// OTHER    ----------------------------------
	
	/**
	  * @param authId Id of the targeted incomplete authentication attempt
	  * @return An access point to that authentication's login
	  */
	def forAuthenticationWithId(authId: Int) = new DbLoginForAuth(authId)
	
	
	// NESTED   ----------------------------------
	
	class DbLoginForAuth(authenticationId: Int) extends UniqueModelAccess[IncompleteAuthLogin]
	{
		override def factory = DbIncompleteAuthLogin.factory
		
		override def condition = model.withAuthenticationId(authenticationId).toCondition
	}
}
