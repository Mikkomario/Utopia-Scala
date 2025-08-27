package utopia.scribe.api.database.access.management.notification

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.IssueNotificationDbModel
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, AccessValues}

/**
  * Used for accessing issue notification values from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessIssueNotificationValues(access: AccessManyColumns) extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue notification database properties
	  */
	val model = IssueNotificationDbModel
	
	/**
	  * Access to issue notification ids
	  */
	lazy val ids = apply(model.index) { _.getInt }
	
	/**
	  * ID of the resolution on which this notification is based
	  */
	lazy val resolutionIds = apply(model.resolutionId) { v => v.getInt }
	
	/**
	  * Time when this issue notification was added to the database
	  */
	lazy val creationTimes = apply(model.created) { v => v.getInstant }
	
	/**
	  * Time when this notification was closed / marked as read
	  */
	lazy val closureTimes = apply(model.closed).flatten { v => v.instant }
}

