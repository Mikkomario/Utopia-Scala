package utopia.scribe.api.database.access.management.notification

import utopia.flow.generic.casting.ValueConversions._
import utopia.scribe.api.database.storable.management.IssueNotificationDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual issue notification values from the DB
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessIssueNotificationValue(access: AccessColumn) extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing issue notification database properties
	  */
	val model = IssueNotificationDbModel
	
	/**
	  * Access to issue notification id
	  */
	lazy val id = apply(model.index).optional { _.int }
	
	/**
	  * ID of the resolution on which this notification is based
	  */
	lazy val resolutionId = apply(model.resolutionId).optional { v => v.int }
	
	/**
	  * Time when this issue notification was added to the database
	  */
	lazy val created = apply(model.created).optional { v => v.instant }
	
	/**
	  * Time when this notification was closed / marked as read
	  */
	lazy val closed = apply(model.closed).optional { v => v.instant }
}

