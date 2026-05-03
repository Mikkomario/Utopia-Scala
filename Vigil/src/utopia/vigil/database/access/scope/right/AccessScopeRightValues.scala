package utopia.vigil.database.access.scope.right

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.nosql.targeting.columns.AccessValues
import utopia.vigil.database.props.scope.ScopeRightDbProps

/**
  * Used for accessing scope right values from the DB
  * @author Mikko Hilpinen
  * @since 01.05.2026, v0.1
  */
trait AccessScopeRightValues extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Access to scope right ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * ID of the granted or accessible scope
	  */
	lazy val scopeIds = apply(model.scopeId) { v => v.getInt }
	
	/**
	  * Time when this scope right was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	
	/**
	  * Whether the linked scope is directly accessible. 
	  * False if the scope is only applied when granting access for other authentication methods.
	  */
	lazy val usables = apply(model.usable) { v => v.getBoolean }
	
	
	// ABSTRACT	--------------------
	
	/**
	  * Interface for accessing scope right database properties
	  */
	def model: ScopeRightDbProps
}

