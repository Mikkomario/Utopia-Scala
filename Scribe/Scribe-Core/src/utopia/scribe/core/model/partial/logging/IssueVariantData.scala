package utopia.scribe.core.model.partial.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.AnyType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.operator.CombinedOrdering
import utopia.flow.time.Now
import utopia.flow.util.Version

import java.time.Instant

object IssueVariantData extends FromModelFactoryWithSchema[IssueVariantData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Ordering that sorts by software version (primarily) and by variant creation time (secondarily)
	  */
	implicit val ord: Ordering[IssueVariantData] = CombinedOrdering(
		Ordering.by { v: IssueVariantData => v.version },
		Ordering.by { v: IssueVariantData => v.created }
	)
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("issueId", IntType, Vector("issue_id")), 
			PropertyDeclaration("version", AnyType), PropertyDeclaration("errorId", IntType, 
			Vector("error_id"), isOptional = true), PropertyDeclaration("details", StringType, 
			isOptional = true), PropertyDeclaration("created", InstantType, isOptional = true)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		IssueVariantData(valid("issueId").getInt, Version(valid("version").getString), valid("errorId").int, 
			valid("details").getString, valid("created").getInstant)
}

/**
  * Represents a specific setting where a problem or an issue occurred
  * @param issueId Id of the issue that occurred
  * @param version The program version in which this issue (variant) occurred
  * @param errorId Id of the error / exception that is associated 
  * with this issue (variant). None if not applicable.
  * @param details Details about this case and/or setting.
  * @param created Time when this case or variant was first encountered
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueVariantData(issueId: Int, version: Version, errorId: Option[Int] = None, 
	details: String = "", created: Instant = Now) 
	extends ModelConvertible
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("issueId" -> issueId, "version" -> version.toString, "errorId" -> errorId, 
			"details" -> details, "created" -> created))
}

