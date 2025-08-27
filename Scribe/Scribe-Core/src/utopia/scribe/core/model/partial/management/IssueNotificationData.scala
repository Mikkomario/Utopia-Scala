package utopia.scribe.core.model.partial.management

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.scribe.core.model.factory.management.IssueNotificationFactory

import java.time.Instant

object IssueNotificationData extends FromModelFactoryWithSchema[IssueNotificationData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("resolutionId", IntType, Single("resolution_id")), 
			PropertyDeclaration("created", InstantType, isOptional = true), PropertyDeclaration("closed", 
			InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueNotificationData(valid("resolutionId").getInt, valid("created").getInstant, 
			valid("closed").instant)
}

/**
  * Represents a notification generated based on a reappeared issue
  * @param resolutionId ID of the resolution on which this notification is based
  * @param created      Time when this issue notification was added to the database
  * @param closed       Time when this notification was closed / marked as read
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class IssueNotificationData(resolutionId: Int, created: Instant = Now, closed: Option[Instant] = None) 
	extends IssueNotificationFactory[IssueNotificationData] with ModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Whether this issue notification has already been deprecated
	  */
	def isDeprecated = closed.isDefined
	
	/**
	  * Whether this issue notification is still valid (not deprecated)
	  */
	def isValid = !isDeprecated
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = Model(Vector("resolutionId" -> resolutionId, "created" -> created, 
		"closed" -> closed))
	
	override def withClosed(closed: Instant) = copy(closed = Some(closed))
	
	override def withCreated(created: Instant) = copy(created = created)
	
	override def withResolutionId(resolutionId: Int) = copy(resolutionId = resolutionId)
}

