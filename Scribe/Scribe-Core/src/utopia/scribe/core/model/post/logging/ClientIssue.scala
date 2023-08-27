package utopia.scribe.core.model.post.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.casting.ValueUnwraps._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, ModelValidationFailedException, PropertyDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.{DurationType, IntType, ModelType, PairType, StringType}
import utopia.flow.generic.model.template.{ModelConvertible, ModelLike, Property}
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{ApproxSelfEquals, EqualsFunction}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.{Mutate, Version}
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.enumeration.Severity

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.util.Try

object ClientIssue extends FromModelFactory[ClientIssue]
{
	// ATTRIBUTES   --------------------------
	
	implicit val areSimilar: EqualsFunction[ClientIssue] = (a, b) => {
		a.context == b.context && a.severity == b.severity && a.variantDetails == b.variantDetails &&
			 a.version == b.version && a.error.~==(b.error) { _ ~== _ }
	}
	
	private lazy val schema: ModelDeclaration = ModelDeclaration(
		PropertyDeclaration("version", StringType),
		PropertyDeclaration("context", StringType),
		PropertyDeclaration("severityLevel", IntType, Vector("severity", "severity_level"), Severity.default.level),
		PropertyDeclaration("variantDetails", ModelType, Vector("variant_details", "variant"), isOptional = true),
		PropertyDeclaration("error", ModelType, isOptional = true),
		PropertyDeclaration("message", StringType, isOptional = true),
		PropertyDeclaration("occurrenceDetails", ModelType, Vector("occurrence_details", "details"), isOptional = true),
		PropertyDeclaration("storeDuration", DurationType, Vector("history", "duration", "store_duration"),
			Duration.Zero),
		PropertyDeclaration("instances", IntType, Vector("count"), 1)
	)
	
	
	// IMPLEMENTED  -------------------------
	
	override def apply(model: ModelLike[Property]): Try[ClientIssue] = schema.validate(model).toTry.flatMap { model =>
		val versionStr = model("version").getString
		Version.findFrom(versionStr).toTry { new ModelValidationFailedException(
			s"Specified version '$versionStr' can't be parsed into a version") }
			.map { version =>
				val storeDuration = model("storeDurations").castTo(PairType, DurationType) match {
					case Left(pairValue) => pairValue.getPair.map { _.getDuration }.sorted.toSpan
					case Right(durationValue) => Span.singleValue(durationValue.getDuration)
				}
				apply(version, model("context"), Severity.fromValue(model("severityLevel")),
					model("variantDetails").getModel,
					model("error").model.flatMap { RecordableError(_).toOption }, model("message"),
					model("occurrenceDetails").getModel, storeDuration, model("instances"))
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
  * @param occurrenceDetails Details about this issue occurrence (optional)
  * @param storeDuration Duration how long this issue was stored locally before sending it to the server.
  *                      If multiple issues are represented, contains a range of durations from
  *                      the minimum to the maximum.
  *                      Default = Zero
  * @param instances The number of issue occurrences represented by this instance/entry
  */
case class ClientIssue(version: Version, context: String, severity: Severity, variantDetails: Model = Model.empty,
                       error: Option[RecordableError] = None, message: String = "",
                       occurrenceDetails: Model = Model.empty,
                       storeDuration: Span[FiniteDuration] = Span.singleValue(Duration.Zero), instances: Int = 1)
	extends ModelConvertible with ApproxSelfEquals[ClientIssue]
{
	// IMPLEMENTED  ---------------------
	
	override def self: ClientIssue = this
	
	override implicit def equalsFunction: EqualsFunction[ClientIssue] = ClientIssue.areSimilar
	
	override def toModel: Model = Model.from(
		"version" -> version.toString, "context" -> context, "severityLevel" -> severity.level,
		"variantDetails" -> variantDetails, "error" -> error, "message" -> message,
		"storeDurations" -> storeDuration.ends, "instances" -> instances
	)
	
	override def toString = {
		val header = s"${storeDuration.ends.map { d=> (Now - d).toLocalDateTime }}: $context${
			variantDetails.mapIfNotEmpty { d => s"/$d" }} ($version)${message.mapIfNotEmpty { msg => s": $msg" }}${
			if (instances > 1) s" ($instances instances)" else "" }"
		error match {
			case Some(error) => s"$header\n$error"
			case None => header
		}
	}
	
	
	// OTHER    ------------------------
	
	/**
	  * @param f A mapping function for the local storage durations of this issue
	  * @return Copy of this issue with mapped storage durations
	  */
	def mapStoreDurations(f: Mutate[FiniteDuration]) = copy(storeDuration = storeDuration.mapEnds(f))
	/**
	  * @param duration Duration how long this issue was stored since the previous store duration update
	  * @return Copy of this issue with updated store duration
	  */
	def delayedBy(duration: FiniteDuration) = mapStoreDurations { _ + duration }
	
	/**
	  * Creates a copy of this issue that has been repeated n times recently
	  * @param times The number of times the issue was repeated
	  * @param overDuration The duration over which the issue was repeated
	  *                     (i.e. duration since the last storeDuration value update)
	  * @return Copy of this issue that has been marked as being repeated 'times' times
	  */
	def repeated(times: Int, overDuration: FiniteDuration) =
		copy(storeDuration = storeDuration.mapEnds { _ + overDuration }, instances = instances + times)
	
	/**
	  * @param key Detail key
	  * @param value Detail value
	  * @return Copy of this issue with additional variant detail
	  */
	def withVariantDetail(key: String, value: Value) =
		copy(variantDetails = variantDetails + (key -> value))
	/**
	  * @param details Additional variant details
	  * @return Copy of this issue with the specified variant details added
	  */
	def withAdditionalVariantDetails(details: Model) = copy(variantDetails = variantDetails ++ details)
	
	/**
	  * @param key   Detail key
	  * @param value Detail value
	  * @param newVariant Whether this details should result in a new issue variant
	  *                   (default = false = details are specific to this occurrence)
	  * @return Copy of this issue with additional variant detail
	  */
	def withDetail(key: String, value: Value, newVariant: Boolean = false) = {
		if (newVariant)
			withVariantDetail(key, value)
		else
			copy(occurrenceDetails = occurrenceDetails + (key -> value))
	}
	/**
	  * @param details Additional variant details
	  * @param newVariant Whether these details should result in a new issue variant
	  *                   (default = false = details are specific to this occurrence)
	  * @return Copy of this issue with the specified details added
	  */
	def withAdditionalDetails(details: Model, newVariant: Boolean = false) = {
		if (newVariant)
			withAdditionalVariantDetails(details)
		else
			copy(occurrenceDetails = occurrenceDetails ++ details)
	}
}