package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.TokenFactory
import utopia.exodus.model.stored.auth.Token
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyTokensAccess
{
	// NESTED	--------------------
	
	private class ManyTokensSubView(override val parent: ManyRowModelAccess[Token], 
		override val filterCondition: Condition) 
		extends ManyTokensAccess with SubView
}

/**
  * A common trait for access points which target multiple tokens at a time
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
trait ManyTokensAccess extends ManyTokensAccessLike[Token, ManyTokensAccess] with ManyRowModelAccess[Token]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A copy of this access point which includes token type information
	  */
	def withTypeInfo = {
		// Doesn't apply non-deprecated condition here in order to avoid applying it twice
		val base = DbTypedTokens.includingHistory
		globalCondition match {
			case Some(condition) => base.filter(condition)
			case None => base
		}
	}
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override def factory = TokenFactory
	
	override def filter(additionalCondition: Condition): ManyTokensAccess = 
		new ManyTokensAccess.ManyTokensSubView(this, additionalCondition)
}

