package utopia.scribe.api.database.access.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.IssueDbModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual issue values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.2
  */
case class AccessIssueValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue database properties
	  */
	val model = IssueDbModel
	
	/**
	  * Access to issue id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * Program context where this issue occurred or was logged. Should be unique.
	  */
	lazy val context = apply(model.context) { v => v.getString }
	
	/**
	  * The estimated severity of this issue
	  */
	lazy val severity = apply(model.severity).optional { v => Severity.findForValue(v) }
	
	/**
	  * Time when this issue first occurred or was first recorded
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
}

