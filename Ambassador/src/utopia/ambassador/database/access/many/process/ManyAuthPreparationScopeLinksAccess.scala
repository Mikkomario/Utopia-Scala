package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.access.many.scope.DbAuthPreparationScopes
import utopia.ambassador.database.factory.process.AuthPreparationScopeLinkFactory
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyAuthPreparationScopeLinksAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyAuthPreparationScopeLinksAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyAuthPreparationScopeLinksAccess
}

/**
  * A common trait for access points which target multiple AuthPreparationScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 26.10.2021
  */
trait ManyAuthPreparationScopeLinksAccess 
	extends ManyAuthPreparationScopeLinksAccessLike[AuthPreparationScopeLink, ManyAuthPreparationScopeLinksAccess] 
		with ManyRowModelAccess[AuthPreparationScopeLink]
{
	// COMPUTED	--------------------
	
	/**
	  * A copy of this access point which returns scope data, also
	  */
	def withScopes = {
		accessCondition match
		{
			case Some(c) => DbAuthPreparationScopes.filter(c)
			case None => DbAuthPreparationScopes
		}
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = AuthPreparationScopeLinkFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyAuthPreparationScopeLinksAccess = 
		ManyAuthPreparationScopeLinksAccess(condition)
}

