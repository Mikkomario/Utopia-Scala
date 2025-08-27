package utopia.scribe.api.database.access.management.aliasing

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.IssueAliasDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing issue alias values from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessIssueAliasValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue alias database properties
	  */
	val model = IssueAliasDbModel
	
	/**
	  * Access to issue alias ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * ID of the described issue
	  */
	lazy val issueIds = apply(model.issueId) { v => v.getInt }
	
	/**
	  * Alias given to the issue. Empty if no alias is given.
	  */
	lazy val aliases = apply(model.alias) { v => v.getString }
	
	/**
	  * New severity level assigned for the issue. None if severity is not modified.
	  */
	lazy val newSeverities = apply(model.newSeverity) { v => v.getInt }
	
	/**
	  * Time when this alias was given
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

