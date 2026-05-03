package utopia.vigil.database.access.scope.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessValue
import utopia.vigil.database.props.scope.ScopeRightDbProps

/**
  * Used for accessing individual scope right values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait AccessScopeRightValue extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to scope right id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * ID of the granted or accessible scope
	  */
	lazy val scopeId = apply(model.scopeId).optional { v => v.int }
	
	/**
	  * Time when this scope right was added to the database
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
	
	/**
	  * Whether the linked scope is directly accessible. 
	  * False if the scope is only applied when granting access for other authentication methods.
	  */
	lazy val usable = apply(model.usable).optional { v => v.boolean }
	
	
	// ABSTRACT	--------------------
	
	/**
	  * Interface for accessing scope right database properties
	  */
	def model: ScopeRightDbProps
}

