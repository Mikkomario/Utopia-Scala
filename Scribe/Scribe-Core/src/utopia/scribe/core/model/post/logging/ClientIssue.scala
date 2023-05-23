package utopia.scribe.core.model.post.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, ModelValidationFailedException, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{DurationType, IntType, ModelType, StringType, VectorType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.util.Version
import utopia.scribe.core.model.cached.logging.StackTrace
import utopia.scribe.core.model.enumeration.Severity

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

object ClientIssue extends FromModelFactory[ClientIssue]
{
	// ATTRIBUTES   --------------------------
	
	private lazy val schema: ModelDeclaration = ModelDeclaration(
		PropertyDeclaration("version", StringType),
		PropertyDeclaration("context", StringType),
		PropertyDeclaration("severityLevel", IntType, Vector("severity", "severity_level"), Severity.default.level),
		PropertyDeclaration("variantDetails", StringType, Vector("variant", "details", "variant_details"),
			isOptional = true),
		PropertyDeclaration("errorMessages", VectorType,
			Vector("errors", "messages", "error", "message", "error_messages"), isOptional = true),
		PropertyDeclaration("stackTrace", ModelType, Vector("stack", "stack_trace"), isOptional = true),
		PropertyDeclaration("storeDuration", DurationType, Vector("history", "duration", "store_duration"),
			Duration.Zero)
	)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(model: ModelLike[Property]): Try[ClientIssue] = schema.validate(model).toTry.flatMap { model =>
		val versionStr = model("version").getString
		Version.findFrom(versionStr).toTry { new ModelValidationFailedException(
			s"Specified version '$versionStr' can't be parsed into a version") }
			.map { version =>
				apply(version, model("context"), Severity.fromValue(model("severityLevel")), model("variantDetails"),
					model("errorMessages").getVector.map { _.getString }.filter { _.nonEmpty }.distinct,
					model("stackTrace").model.flatMap { StackTrace(_).toOption },
					model("storeDuration"))
			}
	}
}

/**
  * Used for transferring client-side issue data to the server-side
  * @author Mikko Hilpinen
  * @since 23.5.2023, v0.1
  * @constructor Creates a new client side issue (occurrence)
  * @param version The version in which this issue occurred
  * @param context Context of the issue logging location
  * @param severity Severity of this issue
  * @param variantDetails Details about this issue variant, to differentiate it from other issues in this context
  * @param errorMessages Error messages associated with this issue. Should be distinct.
  * @param stackTrace Stack trace of this issue, if applicable
  * @param storeDuration Duration how long this issue was stored locally before sending it to the server
  */
case class ClientIssue(version: Version, context: String, severity: Severity, variantDetails: String,
                       errorMessages: Vector[String], stackTrace: Option[StackTrace], storeDuration: FiniteDuration)
	extends ModelConvertible
{
	override def toModel: Model = Model.from(
		"version" -> version.toString, "context" -> context, "severityLevel" -> severity.level,
		"variantDetails" -> variantDetails, "errorMessages" -> errorMessages, "stackTrace" -> stackTrace,
		"storeDuration" -> storeDuration
	)
}