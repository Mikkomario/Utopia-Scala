package utopia.vigil.database.access.token.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vigil.database.access.scope.right.AccessScopeRightValues
import utopia.vigil.database.storable.token.TokenScopeDbModel

/**
  * Used for accessing token scope values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenScopeValues(access: AccessManyColumns) extends AccessScopeRightValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token scope database properties
	  */
	override val model = TokenScopeDbModel
	
	/**
	  * ID of the token that grants or has access to the linked scope
	  */
	lazy val tokenIds = apply(model.tokenId) { v => v.getInt }
}

