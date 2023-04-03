package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.ScopeFactory
import utopia.exodus.model.stored.auth.Scope
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyScopesAccess
{
	// NESTED	--------------------
	
	private class ManyScopesSubView(override val parent: ManyRowModelAccess[Scope], 
		override val filterCondition: Condition) 
		extends ManyScopesAccess with SubView
}

/**
  * A common trait for access points which target multiple scopes at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyScopesAccess extends ManyScopesAccessLike[Scope, ManyScopesAccess] with ManyRowModelAccess[Scope]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this access point which includes token link information
	  */
	def withTokenLinks = globalCondition match {
		case Some(condition) => DbTokenScopes.filter(condition)
		case None => DbTokenScopes
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = ScopeFactory
	
	override def filter(additionalCondition: Condition): ManyScopesAccess = 
		new ManyScopesAccess.ManyScopesSubView(this, additionalCondition)
}

