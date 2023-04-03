package utopia.exodus.database.access.many.auth

import utopia.exodus.database.access.many.auth.ManyTypedTokensAccess.ManyTypedTokensSubView
import utopia.exodus.database.factory.auth.TypedTokenFactory
import utopia.exodus.database.model.auth.TokenTypeModel
import utopia.exodus.model.combined.auth.TypedToken
import utopia.vault.nosql.access.many.model.{ManyModelAccess, ManyRowModelAccess}
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyTypedTokensAccess
{
	private class ManyTypedTokensSubView(override val parent: ManyModelAccess[TypedToken],
	                                     override val filterCondition: Condition)
		extends ManyTypedTokensAccess with SubView
}

/**
  * A common trait for access points which access multiple tokens at a time and include their type information
  * @author Mikko Hilpinen
  * @since 20.2.2022, v4.0
  */
trait ManyTypedTokensAccess
	extends ManyTokensAccessLike[TypedToken, ManyTypedTokensAccess] with ManyRowModelAccess[TypedToken]
{
	// COMPUTED -------------------------------
	
	protected def typeModel = TokenTypeModel
	
	/**
	  * @return A copy of this access point which only targets refresh tokens
	  *         (those that may be used to refresh sessions of some sort)
	  */
	def refreshTokens = filter(typeModel.refreshedTypeIdColumn.isNotNull)
	/**
	  * @return A copy of this access point which only targets tokens that can't be used to refresh other tokens
	  */
	def accessTokens = filter(typeModel.refreshedTypeIdColumn.isNull)
	
	
	// IMPLEMENTED  ---------------------------
	
	override protected def self = this
	
	override def factory = TypedTokenFactory
	
	override def filter(additionalCondition: Condition): ManyTypedTokensAccess =
		new ManyTypedTokensSubView(this, additionalCondition)
}
