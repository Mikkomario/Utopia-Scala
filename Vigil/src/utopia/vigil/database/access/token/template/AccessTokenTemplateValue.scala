package utopia.vigil.database.access.token.template

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vigil.database.storable.token.TokenTemplateDbModel
import utopia.vigil.model.enumeration.ScopeGrantType

/**
  * Used for accessing individual token template values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
case class AccessTokenTemplateValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing token template database properties
	  */
	val model = TokenTemplateDbModel
	
	/**
	  * Access to token template id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Name of this template. May be empty.
	  */
	lazy val name = apply(model.name) { v => v.getString }
	
	/**
	  * Way the scope-granting functions in this template
	  */
	lazy val scopeGrantType = apply(model.scopeGrantType).optional { v => ScopeGrantType.findForValue(v) }
	
	/**
	  * Duration of the created tokens. None if infinite.
	  */
	lazy val duration = apply(model.duration).optional { v => v.long.map { _.millis } }
	
	/**
	  * Time when this token template was added to the database
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
	
	/**
	  * Whether this type of token may be used to revoke itself
	  */
	lazy val canRevokeSelf = apply(model.canRevokeSelf).optional { v => v.boolean }
	
	/**
	  * Whether the parent tokens may be used to revoke these tokens
	  */
	lazy val parentCanRevoke = apply(model.parentCanRevoke).optional { v => v.boolean }
}

