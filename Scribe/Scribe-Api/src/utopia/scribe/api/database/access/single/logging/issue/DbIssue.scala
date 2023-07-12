package utopia.scribe.api.database.access.single.logging.issue

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.model.immutable.Model
import utopia.flow.operator.End.{First, Last}
import utopia.flow.time.Now
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.scribe.api.database.access.single.logging.error_record.DbErrorRecord
import utopia.scribe.api.database.access.single.logging.issue_variant.DbIssueVariant
import utopia.scribe.api.database.factory.logging.IssueFactory
import utopia.scribe.api.database.model.logging.{IssueModel, IssueOccurrenceModel, IssueVariantModel}
import utopia.scribe.core.model.cached.logging.RecordableError
import utopia.scribe.core.model.combined.logging.{DetailedIssue, DetailedIssueVariant}
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Unrecoverable
import utopia.scribe.core.model.partial.logging.{IssueData, IssueOccurrenceData, IssueVariantData}
import utopia.scribe.core.model.post.logging.ClientIssue
import utopia.scribe.core.model.stored.logging.Issue
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.Condition

import java.time.Instant

/**
  * Used for accessing individual issues
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object DbIssue extends SingleRowModelAccess[Issue] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueModel
	
	private def variantModel = IssueVariantModel
	
	private def occurrenceModel = IssueOccurrenceModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted issue
	  * @return An access point to that issue
	  */
	def apply(id: Int) = DbSingleIssue(id)
	
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
	          timeRange: Span[Instant] = Span.singleValue(Now))(implicit connection: Connection,
		version: Version): DetailedIssue =
	{
		// Inserts or finds the matching issue
		val issueResult = store(IssueData(context, severity, timeRange.start))
		// Extracts the stack trace elements from the error, if applicable,
		// and saves them (and the error) to the database
		val errorStoreResult = error.map { DbErrorRecord.store(_) }
		// Stores or pulls the appropriate issue variant
		// The type indicates whether information was just inserted (First) or already existed (Last)
		val ((issue, storedError), variantDependenciesType) = (issueResult match {
			case Right(existingIssue) =>
				errorStoreResult match {
					case Some(error) => error.mapEither { error => existingIssue -> Some(error) }
					case None => Right(existingIssue -> None)
				}
			case Left(newIssue) => Left(newIssue -> errorStoreResult.map { _.either })
		}).eitherAndSide
		val variantData = IssueVariantData(issue.id, version, storedError.map { _.id }, variantDetails.sorted,
			timeRange.start)
		val variant = (variantDependenciesType match {
			// Case: There is a chance that the variant already exists => Checks for duplicates before inserting
			case Last =>
				DbIssueVariant.findMatching(variantData).toRight { variantModel.insert(variantData) }
			// Case: It's impossible that the variant would already exist => Inserts a new variant
			case First => Left(variantModel.insert(variantData))
		}).either
		// Extracts the distinct error messages
		val errorMessages = error match {
			// Case: Error specified => Retrieves the listed messages from the stack
			case Some(error) =>
				message.notEmpty match {
					case Some(message) => (message +: error.messagesIterator).distinct.toVector
					case None => error.messages
				}
			// Case: No error specified => Uses the specified error message, unless empty
			case None => Vector(message).filter { _.nonEmpty }
		}
		// Stores an issue occurrence
		val occurrence = occurrenceModel.insert(IssueOccurrenceData(variant.id, errorMessages,
			occurrenceDetails.sorted, occurrences, timeRange))
		// Combines the data together and returns
		DetailedIssue(issue, Vector(DetailedIssueVariant(variant, storedError, Vector(occurrence))))
	}
	
	/**
	  * Stores an issue to the database. Avoids inserting duplicate information.
	  * @param data The data to store, if new
	  * @param connection Implicit DB Connection
	  * @return Either Right: A matching issue that already existed in the database, 
		or Left: A newly inserted issue
	  */
	def store(data: IssueData)(implicit connection: Connection) = 
		find(model.withContext(data.context).withSeverity(data.severity).toCondition)
			.toRight { model.insert(data) }
	
	/**
	  * Stores a client-side issue to the database.
	  * Avoids storing duplicate information.
	  * @param issue The issue that should be recorded
	  * @param connection Implicit DB connection
	  * @return The recorded issue
	  */
	def store(issue: ClientIssue)(implicit connection: Connection): DetailedIssue = 
		store(issue.context, issue.error, issue.message, issue.severity, issue.variantDetails, issue.occurrenceDetails,
			issue.instances,issue.storeDuration.map { Now - _ })(connection, issue.version)
	
	/**
	  * @param condition Filter condition to apply in addition to this root view's condition. Should yield
	  *  unique issues.
	  * @return An access point to the issue that satisfies the specified condition
	  */
	protected def filterDistinct(condition: Condition) = UniqueIssueAccess(mergeCondition(condition))
}

