package utopia.vigil.database.access.token.template.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.UncertainBoolean
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vigil.database.storable.token.TokenGrantRightDbModel

/**
  * Used for accessing individual token grant right values from the DB
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
case class AccessTokenGrantRightValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token grant right database properties
	  */
	val model = TokenGrantRightDbModel
	
	/**
	  * Access to token grant right id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * ID of the token template that has been given the right to generate new tokens
	  */
	lazy val ownerTemplateId = apply(model.ownerTemplateId).optional { v => v.int }
	
	/**
	  * ID of the template applied to the generated tokens
	  */
	lazy val grantedTemplateId = apply(model.grantedTemplateId).optional { v => v.int }
	
	/**
	  * Whether generating a new token revokes the token used for authorizing that action
	  */
	lazy val revokesOriginal = apply(model.revokesOriginal).optional { v => v.boolean }
	
	/**
	  * Whether earlier generated tokens should all be revoked when generating new tokens. 
	  * Uncertain if this may be controlled manually.
	  */
	lazy val revokesEarlier = apply(model.revokesEarlier) { v => UncertainBoolean(v.boolean) }
}

