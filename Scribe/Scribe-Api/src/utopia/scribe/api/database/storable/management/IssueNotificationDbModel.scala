package utopia.scribe.api.database.storable.management

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.management.IssueNotificationFactory
import utopia.scribe.core.model.partial.management.IssueNotificationData
import utopia.scribe.core.model.stored.management.IssueNotification
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{DeprecatesAfterDefined, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing IssueNotificationDbModel instances and for inserting issue 
  * notifications to the database
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
object IssueNotificationDbModel 
	extends StorableFactory[IssueNotificationDbModel, IssueNotification, IssueNotificationData] 
		with FromIdFactory[Int, IssueNotificationDbModel] with HasIdProperty 
		with IssueNotificationFactory[IssueNotificationDbModel] with DeprecatesAfterDefined
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with resolution ids
	  */
	lazy val resolutionId = property("resolutionId")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	/**
	  * Database property used for interacting with closure times
	  */
	lazy val closed = property("closed")
	
	override val deprecationColumn = closed
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.issueNotification
	
	override def apply(data: IssueNotificationData): IssueNotificationDbModel = 
		apply(None, Some(data.resolutionId), Some(data.created), data.closed)
	
	override def withClosed(closed: Instant) = apply(closed = Some(closed))
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withResolutionId(resolutionId: Int) = apply(resolutionId = Some(resolutionId))
	
	override protected def complete(id: Value, data: IssueNotificationData) = IssueNotification(id.getInt, 
		data)
}

/**
  * Used for interacting with IssueNotifications in the database
  * @param id issue notification database id
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class IssueNotificationDbModel(id: Option[Int] = None, resolutionId: Option[Int] = None, 
	created: Option[Instant] = None, closed: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, IssueNotificationDbModel] 
		with IssueNotificationFactory[IssueNotificationDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(IssueNotificationDbModel.id.name -> id, 
			IssueNotificationDbModel.resolutionId.name -> resolutionId, 
			IssueNotificationDbModel.created.name -> created, IssueNotificationDbModel.closed.name -> closed)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = IssueNotificationDbModel.table
	
	override def withClosed(closed: Instant) = copy(closed = Some(closed))
	
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	override def withResolutionId(resolutionId: Int) = copy(resolutionId = Some(resolutionId))
}

