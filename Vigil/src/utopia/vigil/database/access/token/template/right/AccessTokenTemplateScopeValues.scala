package utopia.vigil.database.access.token.template.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vigil.database.access.scope.right.AccessScopeRightValues
import utopia.vigil.database.storable.token.TokenTemplateScopeDbModel

/**
  * Used for accessing token template scope values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplateScopeValues(access: AccessManyColumns) extends AccessScopeRightValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token template scope database properties
	  */
	override val model = TokenTemplateScopeDbModel
	
	/**
	  * ID of the template that grants this scope
	  */
	lazy val templateIds = apply(model.templateId) { v => v.getInt }
}

