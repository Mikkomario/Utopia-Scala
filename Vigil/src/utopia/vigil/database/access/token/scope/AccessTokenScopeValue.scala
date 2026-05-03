package utopia.vigil.database.access.token.scope

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vigil.database.access.scope.right.AccessScopeRightValue
import utopia.vigil.database.storable.token.TokenScopeDbModel

/**
  * Used for accessing individual token scope values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenScopeValue(access: AccessColumn) extends AccessScopeRightValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token scope database properties
	  */
	override val model = TokenScopeDbModel
	
	/**
	  * ID of the token that grants or has access to the linked scope
	  */
	lazy val tokenId = apply(model.tokenId).optional { v => v.int }
}

