package utopia.vigil.database.access.token.template.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.UncertainBoolean
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}
import utopia.vigil.database.storable.token.TokenGrantRightDbModel

/**
  * Used for accessing token grant right values from the DB
  * @author Mikko Hilpinen
  * @since 04.05.2026, v0.1
  */
case class AccessTokenGrantRightValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token grant right database properties
	  */
	val model = TokenGrantRightDbModel
	
	/**
	  * Access to token grant right ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * ID of the token template that has been given the right to generate new tokens
	  */
	lazy val ownerTemplateIds = apply(model.ownerTemplateId) { v => v.getInt }
	/**
	  * ID of the template applied to the generated tokens
	  */
	lazy val grantedTemplateIds = apply(model.grantedTemplateId) { v => v.getInt }
	/**
	  * Whether generating a new token revokes the token used for authorizing that action
	  */
	lazy val revokeOriginals = apply(model.revokesOriginal) { v => v.getBoolean }
	/**
	  * Whether earlier generated tokens should all be revoked when generating new tokens. 
	  * Uncertain if this may be controlled manually.
	  */
	lazy val revokeEarlier = apply(model.revokesEarlier) { v => UncertainBoolean(v.boolean) }
	
	
	// COMPUTED ------------------------
	
	/**
	 * @param defaultRevokeEarlier Whether to include template IDs for cases where revokesEarlier is uncertain.
	 *                             Default = false.
	 * @param connection Implicit DB connection
	 * @return 2 Values:
	 *              1. Whether any accessible row is marked to revoke the original (calling) token
	 *              1. IDs of the token templates, from which previously generated tokens should be revoked
	 */
	def revokeInfo(defaultRevokeEarlier: => Boolean = false)(implicit connection: Connection) =
		access.streamColumns(model.revokesOriginal, model.revokesEarlier, model.grantedTemplateId) { rowsIter =>
			var revokeOriginal = false
			val revokedTemplateIds = rowsIter
				.flatMap { row =>
					if (!revokeOriginal && row.head.getBoolean)
						revokeOriginal = true
					if (row(1).booleanOr(defaultRevokeEarlier))
						row(2).int
					else
						None
				}
				.toSet
			
			revokeOriginal -> revokedTemplateIds
		}
}

