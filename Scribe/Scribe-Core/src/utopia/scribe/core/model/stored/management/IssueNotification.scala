package utopia.scribe.core.model.stored.management

import utopia.scribe.core.model.factory.management.IssueNotificationFactoryWrapper
import utopia.scribe.core.model.partial.management.IssueNotificationData
import utopia.vault.store.{FromIdFactory, StandardStoredFactory, StoredModelConvertible}

object IssueNotification extends StandardStoredFactory[IssueNotificationData, IssueNotification]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = IssueNotificationData
}

/**
  * Represents a issue notification that has already been stored in the database
  * @param id   id of this issue notification in the database
  * @param data Wrapped issue notification data
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class IssueNotification(id: Int, data: IssueNotificationData) 
	extends StoredModelConvertible[IssueNotificationData] with FromIdFactory[Int, IssueNotification] 
		with IssueNotificationFactoryWrapper[IssueNotificationData, IssueNotification]
{
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: IssueNotificationData) = copy(data = data)
}

