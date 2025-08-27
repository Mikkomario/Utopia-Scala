package utopia.scribe.api.database.access.management.notification

import utopia.scribe.core.model.stored.management.IssueNotification
import utopia.vault.nosql.targeting.columns.HasValues
import utopia.vault.nosql.targeting.one.{AccessOneDeprecatingRoot, AccessOneWrapper, TargetingOne}

object AccessIssueNotification extends AccessOneDeprecatingRoot[AccessIssueNotification[IssueNotification]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val all = AccessIssueNotifications.all.head
}

/**
  * Used for accessing individual issue notifications from the DB at a time
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessIssueNotification[A](wrapped: TargetingOne[Option[A]]) 
	extends AccessOneWrapper[Option[A], AccessIssueNotification[A]] 
		with HasValues[AccessIssueNotificationValue] with FilterIssueNotifications[AccessIssueNotification[A]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessIssueNotificationValue(wrapped)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingOne[Option[A]]) = AccessIssueNotification(newTarget)
}

