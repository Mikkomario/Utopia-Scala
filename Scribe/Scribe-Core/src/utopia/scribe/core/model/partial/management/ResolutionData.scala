package utopia.scribe.core.model.partial.management

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration, Value}
import utopia.flow.generic.model.mutable.DataType.BooleanType
import utopia.flow.generic.model.mutable.DataType.InstantType
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.mutable.DataType.StringType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Version
import utopia.scribe.core.model.factory.management.ResolutionFactory

import java.time.Instant

object ResolutionData extends FromModelFactoryWithSchema[ResolutionData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = ModelDeclaration(Vector(
		PropertyDeclaration("resolvedIssueId", IntType, Single("resolved_issue_id")),
		PropertyDeclaration("commentId", IntType, Single("comment_id"), isOptional = true),
		PropertyDeclaration("versionThreshold", StringType, Single("version_threshold"), isOptional = true),
		PropertyDeclaration("created", InstantType, isOptional = true),
		PropertyDeclaration("deprecates", InstantType, isOptional = true),
		PropertyDeclaration("silences", BooleanType, Empty, false),
		PropertyDeclaration("notifies", BooleanType, Empty, false)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		ResolutionData(valid("resolvedIssueId").getInt, valid("commentId").int, 
			valid("versionThreshold").string.map(Version.apply), valid("created").getInstant,
			valid("deprecates").instant, valid("silences").getBoolean, valid("notifies").getBoolean)
}

/**
  * Marks an issue as resolved (or to be ignored) in some way
  * @param resolvedIssueId  ID of the resolved issue
  * @param commentId        ID of the comment added to this resolution, if applicable
  * @param versionThreshold The first version number (inclusive), to which silencing WON'T apply,
  *                         and for which notifications WILL be generated.
  *                         None if not restricted by version.
  * @param created          Time when this resolution was registered
  * @param deprecates       Time when this resolution expires, was removed or was broken. May be 
  *                         in the future.
  * @param silences         Whether the issue should not be reported while this resolution is 
  *                         active.
  * @param notifies         Whether a notification should be generated if this resolution is 
  *                         broken. 
  *                         Note: Only one notification may be generated in total.
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class ResolutionData(resolvedIssueId: Int, commentId: Option[Int] = None, versionThreshold: Option[Version] = None,
                          created: Instant = Now, deprecates: Option[Instant] = None, silences: Boolean = false,
                          notifies: Boolean = false)
	extends ResolutionFactory[ResolutionData] with ModelConvertible
{
	// COMPUTED ------------------------
	
	/**
	 * @return Whether this is to be considered a still valid / active resolution
	 */
	def isValid = deprecates.forall { _.isFuture }
	
	
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("resolvedIssueId" -> resolvedIssueId, "commentId" -> commentId, 
			"versionThreshold" -> (versionThreshold match { case Some(v) => v.toString; case None => Value.empty }), 
			"created" -> created, "deprecates" -> deprecates, "silences" -> silences, "notifies" -> notifies))
	
	override def withCommentId(commentId: Int) = copy(commentId = Some(commentId))
	override def withCreated(created: Instant) = copy(created = created)
	override def withDeprecates(deprecates: Instant) = copy(deprecates = Some(deprecates))
	override def withNotifies(notifies: Boolean) = copy(notifies = notifies)
	override def withResolvedIssueId(resolvedIssueId: Int) = copy(resolvedIssueId = resolvedIssueId)
	override def withSilences(silences: Boolean) = copy(silences = silences)
	override def withVersionThreshold(versionThreshold: Version) = copy(versionThreshold = 
		Some(versionThreshold))
	
	
	// OTHER    -----------------------
	
	/**
	 * @param currentVersion Current target application version. Call-by-name, called if a version check is necessary.
	 * @return Whether this resolution applies active silencing, affecting the specified version.
	 */
	def silencesVersion(currentVersion: => Version) =
		silences && isValid && versionThreshold.forall { _ > currentVersion }
}

