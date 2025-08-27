package utopia.scribe.api.database.access.management.notification

import utopia.scribe.api.database.reader.management.IssueNotificationDbReader
import utopia.scribe.api.database.storable.management.IssueNotificationDbModel
import utopia.scribe.core.model.stored.management.IssueNotification
import utopia.vault.nosql.targeting.columns.{AccessManyColumns, HasValues}
import utopia.vault.nosql.targeting.many.{AccessManyDeprecatingRoot, AccessRowsWrapper, AccessWrapper, DeprecatingWrapRowAccess, TargetingMany, TargetingManyLike, TargetingManyRows, TargetingTimeline, WrapOneToManyAccess}
import utopia.vault.nosql.targeting.one.TargetingOne

object AccessIssueNotifications 
	extends DeprecatingWrapRowAccess[AccessIssueNotificationRows](IssueNotificationDbModel) 
		with WrapOneToManyAccess[AccessCombinedIssueNotifications] 
		with AccessManyDeprecatingRoot[AccessIssueNotificationRows[IssueNotification]]
{
	// ATTRIBUTES	--------------------
	
	override lazy val all = apply(IssueNotificationDbReader).all
	
	
	// IMPLEMENTED	--------------------
	
	override def apply[A](access: TargetingMany[A]) = AccessCombinedIssueNotifications(access)
	
	override protected def wrap[A](access: TargetingManyRows[A]) = AccessIssueNotificationRows(access)
}

/**
  * Used for accessing multiple issue notifications from the DB at a time
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
abstract class AccessIssueNotifications[A, +Repr <: TargetingManyLike[_, Repr, 
	_]](wrapped: AccessManyColumns) 
	extends TargetingTimeline[A, Repr, AccessIssueNotification[A]] 
		with HasValues[AccessIssueNotificationValues] with FilterIssueNotifications[Repr]
{
	// ATTRIBUTES	--------------------
	
	override lazy val values = AccessIssueNotificationValues(wrapped)
}

/**
  * Provides access to row-specific issue notification -like items
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessIssueNotificationRows[A](wrapped: TargetingManyRows[A]) 
	extends AccessIssueNotifications[A, AccessIssueNotificationRows[A]](wrapped) 
		with AccessRowsWrapper[A, AccessIssueNotificationRows[A], AccessIssueNotification[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingManyRows[A]) = AccessIssueNotificationRows(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueNotification(target)
}

/**
  * Used for accessing issue notification items that have been combined with one-to-many 
  * combinations
  * @param wrapped The wrapped access point
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class AccessCombinedIssueNotifications[A](wrapped: TargetingMany[A]) 
	extends AccessIssueNotifications[A, AccessCombinedIssueNotifications[A]](wrapped) 
		with AccessWrapper[A, AccessCombinedIssueNotifications[A], AccessIssueNotification[A]]
{
	// IMPLEMENTED	--------------------
	
	override protected def self = this
	
	override protected def wrap(newTarget: TargetingMany[A]) = AccessCombinedIssueNotifications(newTarget)
	
	override protected def wrapUniqueTarget(target: TargetingOne[Option[A]]) = AccessIssueNotification(target)
}

