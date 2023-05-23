package utopia.scribe.core.model.post.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, ModelValidationFailedException, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.{DurationType, IntType, ModelType, StringType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.util.Version
import utopia.scribe.core.model.cached.logging.RecordableError
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
		PropertyDeclaration("error", ModelType, isOptional = true),
		PropertyDeclaration("message", StringType, isOptional = true),
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
					model("error").model.flatMap { RecordableError(_).toOption }, model("message"),
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
  * @param error The error that occurred, if applicable
  * @param message Additional error message (optional)
  * @param storeDuration Duration how long this issue was stored locally before sending it to the server
  */
case class ClientIssue(version: Version, context: String, severity: Severity, variantDetails: String,
                       error: Option[RecordableError], message: String, storeDuration: FiniteDuration)
	extends ModelConvertible
{
	// IMPLEMENTED  ---------------------
	
	override def toModel: Model = Model.from(
		"version" -> version.toString, "context" -> context, "severityLevel" -> severity.level,
		"variantDetails" -> variantDetails, "error" -> error, "message" -> message, "storeDuration" -> storeDuration
	)
	
	
	// OTHER    ------------------------
	
	/**
	  * @param duration Duration how long this issue was stored since the previous store duration update
	  * @return Copy of this issue with updated store duration
	  */
	def delayedBy(duration: FiniteDuration) = copy(storeDuration = storeDuration + duration)
}