package utopia.scribe.api.database.access.management.aliasing

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.IssueAliasDbModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual issue alias values from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2025, v1.2
  */
case class AccessIssueAliasValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue alias database properties
	  */
	val model = IssueAliasDbModel
	
	/**
	  * Access to issue alias id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * ID of the described issue
	  */
	lazy val issueId = apply(model.issueId).optional { v => v.int }
	
	/**
	  * Alias given to the issue. Empty if no alias is given.
	  */
	lazy val alias = apply(model.alias) { v => v.getString }
	
	/**
	  * New severity level assigned for the issue. None if severity is not modified.
	  */
	lazy val newSeverity = apply(model.newSeverity).optional { v => Severity.findForValue(v) }
	
	/**
	  * Time when this alias was given
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

