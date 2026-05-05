package utopia.vigil.database.access.token

import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.Now
import utopia.vault.database.Connection
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vigil.database.storable.token.TokenDbModel

/**
  * Used for accessing individual token values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token database properties
	  */
	val model = TokenDbModel
	
	/**
	  * Access to token id
	  */
	lazy val id = apply(model.index).optional { _.int }
	/**
	  * ID of the template used when creating this token
	  */
	lazy val templateId = apply(model.templateId).optional { v => v.int }
	/**
	  * Hashed version of this token
	  */
	lazy val hash = apply(model.hash) { v => v.getString }
	/**
	  * ID of the token that was used to generate this token
	  */
	lazy val parentId = apply(model.parentId).optional { v => v.int }
	/**
	  * Name of this token. May be empty.
	  */
	lazy val name = apply(model.name) { v => v.getString }
	/**
	  * Time when this token was created
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
	/**
	  * Time when this token automatically expires. None if this token doesn't expire automatically.
	  */
	lazy val expires = apply(model.expires).optional { v => v.instant }
	/**
	  * Time when this token was revoked.
	  */
	lazy val revoked = apply(model.revoked).optional { v => v.instant }
	
	
	// COMPUTED ---------------------------
	
	/**
	 * @param connection Implicit DB connection
	 * @return An iterator that yields IDs of this token's parents, starting from the closest
	 */
	def parentIdsIterator(implicit connection: Connection) =
		OptionsIterator.iterate(parentId.pull) { AccessToken(_).parentId.pull }
	/**
	 * @param connection Implicit DB connection
	 * @return An iterator that yields the IDs of this token's active (i.e. unrevoked & unexpired) parents,
	 *         starting from the closest
	 */
	def activeParentIdsIterator(implicit connection: Connection) =
		parentIdsIterator.filter { AccessToken(_).active.nonEmpty }
	
	
	// OTHER    ---------------------------
	
	/**
	 * Revokes the accessible token
	 * @param connection Implicit DB connection
	 * @return Whether a token was targeted
	 */
	def revoke()(implicit connection: Connection) = revoked.set(Now)
}

