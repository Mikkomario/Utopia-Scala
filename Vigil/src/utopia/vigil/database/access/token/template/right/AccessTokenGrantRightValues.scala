package utopia.vigil.database.access.token.template.right

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}
import utopia.vigil.database.storable.token.TokenGrantRightDbModel

/**
  * Used for accessing token grant right values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
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
	lazy val revokeOriginals = apply(model.revokes) { v => v.getBoolean }
	
	
	// COMPUTED -----------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return Returns 2 values:
	 *         1. Template IDs that may be used for constructing new tokens, based on this token.
	 *         1. Whether the original token should be revoked afterwards
	 */
	def grantedTemplateIdsAndRevokes(implicit connection: Connection) =
		access.streamColumns(model.grantedTemplateId, model.revokes) { rowsIter =>
			var revokes = false
			val grantedTemplateIds = rowsIter
				.map { row =>
					if (!revokes && row(1).getBoolean)
						revokes = true
					row.head.getInt
				}
				.toOptimizedSeq
			grantedTemplateIds -> revokes
		}
}

