package utopia.scribe.api.database.access.management.notification

import utopia.vault.model.immutable.Table
import utopia.vault.nosql.view.{FilterableView, FilterableViewWrapper}

/**
  * An interface which provides issue notification -based filtering for other types of access 
  * points.
  * @param wrapped Wrapped access point. Expected to include issue_notification.
  * @tparam A Type of the wrapped access class
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class FilterByIssueNotification[+A <: FilterableView[A]](wrapped: A) 
	extends FilterIssueNotifications[A] with FilterableViewWrapper[A]
{
	override def table: Table = model.table
}