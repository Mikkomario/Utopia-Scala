package utopia.exodus.database.access.many.auth

import utopia.exodus.database.factory.auth.TypedTokenFactory
import utopia.exodus.database.model.auth.TokenTypeModel
import utopia.exodus.model.combined.auth.TypedToken
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.Condition

object ManyTypedTokensAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyTypedTokensAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyTypedTokensAccess
}

/**
  * A common trait for access points which access multiple tokens at a time and include their type information
  * @author Mikko Hilpinen
  * @since 20.02.2022, v4.0
  */
trait ManyTypedTokensAccess 
	extends ManyTokensAccessLike[TypedToken, ManyTypedTokensAccess] with ManyRowModelAccess[TypedToken]
{
	// COMPUTED	--------------------
	
	/**
	  * A copy of this access point which only targets refresh tokens
	  * (those that may be used to refresh sessions of some sort)
	  */
	def refreshTokens = filter(typeModel.refreshedTypeIdColumn.isNotNull)
	
	/**
	  * A copy of this access point which only targets tokens that can't be used to refresh other tokens
	  */
	def accessTokens = filter(typeModel.refreshedTypeIdColumn.isNull)
	
	protected def typeModel = TokenTypeModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = TypedTokenFactory
	
	override protected def self = this
	
	override def apply(condition: Condition): ManyTypedTokensAccess = ManyTypedTokensAccess(condition)
}

