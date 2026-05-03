package utopia.vigil.database.storable.token

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vigil.database.VigilTables
import utopia.vigil.database.props.scope.ScopeRightDbProps
import utopia.vigil.database.storable.scope.{ScopeRightDbModel, ScopeRightDbModelFactoryLike, ScopeRightDbModelLike}
import utopia.vigil.model.factory.token.TokenScopeFactory
import utopia.vigil.model.partial.token.TokenScopeData
import utopia.vigil.model.stored.token.TokenScope

import java.time.Instant

/**
  * Used for constructing TokenScopeDbModel instances and for inserting token scopes to the 
  * database
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
object TokenScopeDbModel 
	extends ScopeRightDbModelFactoryLike[TokenScopeDbModel, TokenScope, TokenScopeData] 
		with TokenScopeFactory[TokenScopeDbModel] with ScopeRightDbProps
{
	// ATTRIBUTES	--------------------
	
	override val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with scope ids
	  */
	override lazy val scopeId = property("scopeId")
	
	/**
	  * Database property used for interacting with token ids
	  */
	lazy val tokenId = property("tokenId")
	
	/**
	  * Database property used for interacting with creation times
	  */
	override lazy val created = property("created")
	
	/**
	  * Database property used for interacting with usables
	  */
	override lazy val usable = property("usable")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = VigilTables.tokenScope
	
	override def apply(data: TokenScopeData): TokenScopeDbModel = 
		apply(None, Some(data.scopeId), Some(data.tokenId), Some(data.created), Some(data.usable))
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withScopeId(scopeId: Int) = apply(scopeId = Some(scopeId))
	
	override def withTokenId(tokenId: Int) = apply(tokenId = Some(tokenId))
	
	override def withUsable(usable: Boolean) = apply(usable = Some(usable))
	
	override protected def complete(id: Value, data: TokenScopeData) = TokenScope(id.getInt, data)
}

/**
  * Used for interacting with TokenScopes in the database
  * @param id token scope database id
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenScopeDbModel(id: Option[Int] = None, scopeId: Option[Int] = None, 
	tokenId: Option[Int] = None, created: Option[Instant] = None, usable: Option[Boolean] = None) 
	extends ScopeRightDbModel with ScopeRightDbModelLike[TokenScopeDbModel] 
		with TokenScopeFactory[TokenScopeDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		super[ScopeRightDbModelLike].valueProperties ++ Single(TokenScopeDbModel.tokenId.name -> tokenId)
	
	
	// IMPLEMENTED	--------------------
	
	override def dbProps = TokenScopeDbModel
	
	override def table = TokenScopeDbModel.table
	
	/**
	  * @param id      Id to assign to the new model (default = currently assigned id)
	  * @param scopeId scope id to assign to the new model (default = currently assigned value)
	  * @param created created to assign to the new model (default = currently assigned value)
	  * @param usable  usable to assign to the new model (default = currently assigned value)
	  */
	override def copyScopeRight(id: Option[Int] = id, scopeId: Option[Int] = scopeId, 
		created: Option[Instant] = created, usable: Option[Boolean] = usable) = 
		copy(id = id, scopeId = scopeId, created = created, usable = usable)
	
	override def withTokenId(tokenId: Int) = copy(tokenId = Some(tokenId))
}

