package utopia.scribe.api.database.access.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.logging.IssueDbModel
import utopia.scribe.core.model.enumeration.Severity
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing issue values from the DB
  * @author Mikko Hilpinen
  * @since 27.07.2025, v1.1
  */
case class AccessIssueValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue database properties
	  */
	val model = IssueDbModel
	
	/**
	  * Access to issue ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * Program context where this issue occurred or was logged. Should be unique.
	  */
	lazy val contexts = apply(model.context) { v => v.getString }
	
	/**
	  * The estimated severity of this issue
	  */
	lazy val severities = apply(model.severity) { v => Severity.fromValue(v) }
	
	/**
	  * Time when this issue first occurred or was first recorded
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
}

