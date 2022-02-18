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
	// IMPLEMENTED	--------------------
	
	override def factory = TokenFactory
	
	override def filter(additionalCondition: Condition): ManyTokensAccess =
		new ManyTokensAccess.ManyTokensSubView(this, additionalCondition)
}

