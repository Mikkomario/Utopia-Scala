package utopia.vigil.database.access.token

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}
import utopia.vigil.database.storable.token.TokenDbModel

/**
  * Used for accessing token values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token database properties
	  */
	val model = TokenDbModel
	
	/**
	  * Access to token ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * ID of the template used when creating this token
	  */
	lazy val templateIds = apply(model.templateId) { v => v.getInt }
	/**
	  * Hashed version of this token
	  */
	lazy val hashes = apply(model.hash) { v => v.getString }
	/**
	  * ID of the token that was used to generate this token
	  */
	lazy val parentIds = apply(model.parentId).flatten { v => v.int }
	/**
	  * Name of this token. May be empty.
	  */
	lazy val names = apply(model.name) { v => v.getString }
	/**
	  * Time when this token was created
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	/**
	  * Time when this token automatically expires. None if this token doesn't expire automatically.
	  */
	lazy val expirationTimes = apply(model.expires).flatten { v => v.instant }
	/**
	  * Time when this token was revoked.
	  */
	lazy val revokeTimes = apply(model.revoked).flatten { v => v.instant }
	
	
	// OTHER    -------------------------
	
	/**
	 * Revokes all accessible tokens
	 * @param connection Implicit DB connection
	 * @return Whether any row was targeted
	 */
	def revoke()(implicit connection: Connection) = revokeTimes.set(Now)
}

