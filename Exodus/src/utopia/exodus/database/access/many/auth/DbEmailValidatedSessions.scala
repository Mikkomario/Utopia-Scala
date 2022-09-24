package utopia.exodus.database.access.many.auth

import utopia.exodus.model.stored.auth.EmailValidatedSession
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.view.NonDeprecatedView
import utopia.vault.sql.SqlExtensions._

/**
  * The root access point when targeting multiple EmailValidatedSessions at a time
  * @author Mikko Hilpinen
  * @since 24.11.2021, v3.1
  */
@deprecated("Will be removed in a future release", "v4.0")
object DbEmailValidatedSessions 
	extends ManyEmailValidatedSessionsAccess with NonDeprecatedView[EmailValidatedSession]
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted EmailValidatedSessions
	  * @return An access point to EmailValidatedSessions with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbEmailValidatedSessionsSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbEmailValidatedSessionsSubset(targetIds: Set[Int]) extends ManyEmailValidatedSessionsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(index in targetIds)
	}
}

