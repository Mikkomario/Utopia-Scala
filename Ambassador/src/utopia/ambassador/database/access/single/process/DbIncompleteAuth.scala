package utopia.ambassador.database.access.single.process

import utopia.ambassador.database.factory.process.IncompleteAuthFactory
import utopia.ambassador.database.model.process.IncompleteAuthModel
import utopia.ambassador.model.stored.process.IncompleteAuth
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual incomplete authentications in the DB
  * @author Mikko Hilpinen
  * @since 19.7.2021, v1.0
  */
object DbIncompleteAuth extends SingleRowModelAccess[IncompleteAuth] with NonDeprecatedView[IncompleteAuth]
{
	// COMPUTED --------------------------------
	
	private def model = IncompleteAuthModel
	
	
	// IMPLEMENTED  ----------------------------
	
	override def factory = IncompleteAuthFactory
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param authId An authentication id
	  * @return An access point to that authentication's data
	  */
	def apply(authId: Int) = new DbSingleIncompleteAuth(authId)
	
	/**
	  * @param incompleteAuthToken An authentication token
	  * @param connection Implicit DB Connection
	  * @return An open incomplete auth case with that token
	  */
	def forToken(incompleteAuthToken: String)(implicit connection: Connection) =
		find(model.withToken(incompleteAuthToken).toCondition)
	
	
	// NESTED   ---------------------------------
	
	class DbSingleIncompleteAuth(override val id: Int) extends SingleIntIdModelAccess[IncompleteAuth]
	{
		// COMPUTED -----------------------------
		
		/**
		  * @return An access point to this authentication's login
		  */
		def login = DbIncompleteAuthLogin.forAuthenticationWithId(id)
		
		/**
		  * @param connection Implicit DB connection
		  * @return Whether this authentication attempt has been completed already
		  */
		def isClosed(implicit connection: Connection) = login.nonEmpty
		
		
		// IMPLEMENTED  -----------------------
		
		override def factory = DbIncompleteAuth.factory
	}
}
