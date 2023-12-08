package utopia.ambassador.database.access.many.process

import utopia.ambassador.database.access.many.scope.DbAuthPreparationScopes
import utopia.ambassador.database.factory.process.AuthPreparationScopeLinkFactory
import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyAuthPreparationScopeLinksAccess
{
	// NESTED	--------------------
	
	private class ManyAuthPreparationScopeLinksSubView(override val parent: ManyRowModelAccess[AuthPreparationScopeLink],
		override val filterCondition: Condition)
		extends ManyAuthPreparationScopeLinksAccess with SubView
}

/**
  * A common trait for access points which target multiple AuthPreparationScopeLinks at a time
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
trait ManyAuthPreparationScopeLinksAccess
	extends ManyAuthPreparationScopeLinksAccessLike[AuthPreparationScopeLink, ManyAuthPreparationScopeLinksAccess]
		with ManyRowModelAccess[AuthPreparationScopeLink]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this access point which returns scope data, also
	  */
	def withScopes = accessCondition match
	{
		case Some(c) => DbAuthPreparationScopes.filter(c)
		case None => DbAuthPreparationScopes
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = AuthPreparationScopeLinkFactory
	
	override def _filter(additionalCondition: Condition): ManyAuthPreparationScopeLinksAccess =
		new ManyAuthPreparationScopeLinksAccess.ManyAuthPreparationScopeLinksSubView(this,
			additionalCondition)
}

