package utopia.vigil.model.partial.token

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.time.Now
import utopia.vigil.model.cached.scope.HasScopeId
import utopia.vigil.model.factory.token.TokenScopeFactory
import utopia.vigil.model.partial.scope.{ScopeRightData, ScopeRightDataLike}

import java.time.Instant

object TokenScopeData extends FromModelFactoryWithSchema[TokenScopeData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("scopeId", IntType, Single("scope_id")), 
			PropertyDeclaration("tokenId", IntType, Single("token_id")), PropertyDeclaration("created", 
			InstantType, isOptional = true), PropertyDeclaration("usable", BooleanType, Empty, false)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		TokenScopeData(valid("scopeId").getInt, valid("tokenId").getInt, valid("created").getInstant, 
			valid("usable").getBoolean)
}

/**
  * Allows a token to be used in some scope
  * @param scopeId ID of the granted or accessible scope
  * @param tokenId ID of the token that grants or has access to the linked scope
  * @param created Time when this scope right was added to the database
  * @param usable  Whether the linked scope is directly accessible. 
  *                False if the scope is only applied when granting access for other 
  *                authentication methods.
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class TokenScopeData(scopeId: Int, tokenId: Int, created: Instant = Now, usable: Boolean = false) 
	extends TokenScopeFactory[TokenScopeData] with ScopeRightData with ScopeRightDataLike[TokenScopeData]
		with HasScopeId
{
	// IMPLEMENTED	--------------------
	
	override def toModel = super[ScopeRightData].toModel ++ Model(Single("tokenId" -> tokenId))
	
	override def copyScopeRight(scopeId: Int, created: Instant, usable: Boolean) = 
		copy(scopeId = scopeId, created = created, usable = usable)
	
	override def withTokenId(tokenId: Int) = copy(tokenId = tokenId)
}

