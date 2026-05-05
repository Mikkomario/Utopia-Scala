package utopia.vigil.database.access.token.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}
import utopia.vigil.database.VigilContext
import utopia.vigil.database.storable.token.TokenTemplateDbModel
import utopia.vigil.model.enumeration.ScopeGrantType

/**
  * Used for accessing token template values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplateValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token template database properties
	  */
	val model = TokenTemplateDbModel
	
	/**
	  * Access to token template ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	/**
	  * Name of this template. May be empty.
	  */
	lazy val names = apply(model.name) { v => v.getString }
	/**
	  * Way the scope-granting functions in this template
	  */
	lazy val scopeGrantTypes = VigilContext.log.use { implicit log =>
		apply(model.scopeGrantType).logging { v => ScopeGrantType.fromValue(v) }
	}
	/**
	  * Duration of the created tokens. None if infinite.
	  */
	lazy val durations = apply(model.duration).flatten { v => v.long.map { _.millis } }
	/**
	  * Time when this token template was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	/**
	  * Whether this type of token may be used to revoke itself
	  */
	lazy val canRevokeSelves = apply(model.canRevokeSelf) { v => v.getBoolean }
	/**
	  * Whether the parent tokens may be used to revoke these tokens
	  */
	lazy val parentsCanRevoke = apply(model.parentCanRevoke) { v => v.getBoolean }
}

