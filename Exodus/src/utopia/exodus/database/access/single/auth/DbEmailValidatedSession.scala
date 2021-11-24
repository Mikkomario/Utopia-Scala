package utopia.exodus.database.access.single.auth

import utopia.exodus.database.factory.auth.EmailValidatedSessionFactory
import utopia.exodus.database.model.auth.EmailValidatedSessionModel
import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.{NonDeprecatedView, SubView}

/**
  * Used for accessing individual EmailValidatedSessions
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
object DbEmailValidatedSession 
	extends SingleRowModelAccess[EmailValidatedSession] with NonDeprecatedView[EmailValidatedSession] 
		with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = EmailValidatedSessionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = EmailValidatedSessionFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted EmailValidatedSession instance
	  * @return An access point to that EmailValidatedSession
	  */
	def apply(id: Int) = DbSingleEmailValidatedSession(id)
	
	/**
	  * @param token An email session token
	  * @return An access point to that session in the database, if it is open
	  */
	def withToken(token: String) = new DbEmailValidatedSessionWithToken(token)
	
	
	// NESTED   --------------------
	
	class DbEmailValidatedSessionWithToken(token: String) extends UniqueEmailValidatedSessionAccess with SubView
	{
		override protected def parent = DbEmailValidatedSession
		override def filterCondition = model.withToken(token).toCondition
		override protected def defaultOrdering = Some(factory.defaultOrdering)
	}
}

