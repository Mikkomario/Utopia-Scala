package utopia.scribe.core.model.factory.management

import java.time.Instant

/**
  * Common trait for issue notification-related factories which allow construction with 
  * individual properties
  * @tparam A Type of constructed instances
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
trait IssueNotificationFactory[+A]
{
	// ABSTRACT	--------------------
	
	/**
	  * @param closed New closed to assign
	  * @return Copy of this item with the specified closed
	  */
	def withClosed(closed: Instant): A
	
	/**
	  * @param created New created to assign
	  * @return Copy of this item with the specified created
	  */
	def withCreated(created: Instant): A
	
	/**
	  * @param resolutionId New resolution id to assign
	  * @return Copy of this item with the specified resolution id
	  */
	def withResolutionId(resolutionId: Int): A
}

