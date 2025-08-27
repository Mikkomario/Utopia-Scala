package utopia.scribe.api.database.access.logging.issue

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Single
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.scribe.api.database.access.logging.error.ErrorDb
import utopia.scribe.api.database.access.logging.issue.variant.AccessIssueVariants
import utopia.scribe.api.database.access.management.resolution.AccessResolutions
import utopia.scribe.api.database.storable.logging.{IssueDbModel, IssueOccurrenceDbModel, IssueVariantDbModel}
import utopia.scribe.api.database.storable.management.IssueNotificationDbModel
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.combined.logging.{DetailedIssueVariant, IssueWithDetailedVariants}
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Unrecoverable
import utopia.scribe.core.model.partial.logging.{IssueData, IssueOccurrenceData, IssueVariantData}
import utopia.scribe.core.model.partial.management.IssueNotificationData
import utopia.scribe.core.model.post.logging.ClientIssue
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.database.Connection
import utopia.vault.store.StoreResult

import java.time.Instant

/**
  * Provides functions for interacting with issues in the DB
  * @author Mikko Hilpinen
  * @since 29.07.2025, v1.0.6
  */
object IssueDb
{
	// ATTRIBUTES   -----------------------
	
	private val model = IssueDbModel
	private val variantModel = IssueVariantDbModel
	private val occurrenceModel = IssueOccurrenceDbModel
	private val notificationModel = IssueNotificationDbModel
	
	
	// OTHER    ---------------------------
	
	/**
	  * Finds an issue variant
	  * @param issueId ID of the targeted issue
	  * @param version Affected version
	  * @param errorId ID of the associated error, if applicable
	  * @param details Variant details, if applicable
	  * @param connection Implicit DB connection
	  * @return Matching issue variant. None if no variant matched the specified info.
	  */
	def findVariantMatching(issueId: Int, version: Version, errorId: Option[Int] = None, details: Model = Model.empty)
	                       (implicit connection: Connection) =
	{
		// Doesn't apply the details condition to the query, because that column is not indexed
		AccessIssueVariants.ofIssue(issueId).affectingVersion(version).causedBy(errorId).pull
			.find { _.details ~== details }
	}
	
	/**
	  * Stores a detailed issue to the database.
	  * Avoids inserting duplicate information.
	  * @param context The context in which this issue occurred
	  * @param error The error causing this issue, if applicable
	  * @param message A message concerning this issue (may be specific to this occurrence) (optional)
	  * @param severity The severity of this issue (default = Unrecoverable)
	  * @param variantDetails Some details about this issue variant (optional).
	  *                       Please note that different details result in different variants being stored.
	  * @param occurrenceDetails Details about this or these specific issue occurrences.
	  *                          Different details will not result in different issue variants.
	  *                          Default = empty.
	  * @param occurrences The number of specific occurrences that are represented here (default = 1)
	  * @param timeRange The time range within which this issue occurred (default = Now)
	  * @param connection Implicit DB connection
	  * @param version Applicable version (implicit)
	  * @return Stored issue, including pulled or inserted information.
	  * Only contains information about this specific variant and occurrence.
	  */
	def store(context: String, error: Option[RecordableError] = None, message: String = "",
	          severity: Severity = Unrecoverable, variantDetails: Model = Model.empty,
	          occurrenceDetails: Model = Model.empty, occurrences: Int = 1,
	          timeRange: Span[Instant] = Span.singleValue(Now))
	         (implicit connection: Connection, version: Version): IssueWithDetailedVariants =
	{
		// Stores the root issue and the associated error
		val issue = store(IssueData(context, severity, timeRange.start))
		val storedError = error.map { ErrorDb.store(_) }
		
		// Stores or pulls the appropriate issue variant
		val variant = {
			def insert() = variantModel.insert(IssueVariantData(
				issue.id, version, storedError.map { _.id }, variantDetails.sorted, timeRange.start))
			
			if (issue.isNew || storedError.exists { _.isNew })
				insert()
			else
				findVariantMatching(issue.id, version, storedError.map { _.id }, variantDetails).getOrElse { insert() }
		}
		
		// Extracts the distinct error messages
		val errorMessages = error match {
			// Case: Error specified => Retrieves the listed messages from the stack
			case Some(error) =>
				message.ifNotEmpty match {
					case Some(message) => (message +: error.messagesIterator).distinct.toOptimizedSeq
					case None => error.messages
				}
			// Case: No error specified => Uses the specified error message, unless empty
			case None => Single(message).filter { _.nonEmpty }
		}
		// Stores an issue occurrence
		val occurrence = occurrenceModel.insert(IssueOccurrenceData(variant.id, errorMessages,
			occurrenceDetails.sorted, occurrences, timeRange))
		
		// Checks whether a notification should be generated, also
		val brokenResolutions = AccessResolutions.active.ofIssue(issue.id).generatingNotifications
			.idsAndVersionThresholds.filter { _._2.forall { _ <= version } }
		if (brokenResolutions.nonEmpty) {
			AccessResolutions(brokenResolutions.map { _._1 }).deprecate()
			notificationModel.insert(
				brokenResolutions.map { case (resolutionId, _) => IssueNotificationData(resolutionId) })
		}
		
		// Combines the data together and returns
		IssueWithDetailedVariants(issue, Single(DetailedIssueVariant(variant, storedError.map { _.stored }, Single(occurrence))))
	}
	
	/**
	  * Stores an issue to the database. Avoids inserting duplicate information.
	  * @param data The data to store, if new
	  * @param connection Implicit DB Connection
	  * @return The stored issue. Notes whether newly inserted.
	  */
	def store(data: IssueData)(implicit connection: Connection): StoreResult[Issue] =
		AccessIssue.inContext(data.context).ofSeverity(data.severity).pull.toRight { model.insert(data) }
	
	/**
	  * Stores a client-side issue to the database.
	  * Avoids storing duplicate information.
	  * @param issue The issue that should be recorded
	  * @param connection Implicit DB connection
	  * @return The recorded issue
	  */
	def store(issue: ClientIssue)(implicit connection: Connection): IssueWithDetailedVariants =
		store(issue.context, issue.error, issue.message, issue.severity, issue.variantDetails, issue.occurrenceDetails,
			issue.instances,issue.storeDuration.mapTo { Now - _ })(connection, issue.version)
}
