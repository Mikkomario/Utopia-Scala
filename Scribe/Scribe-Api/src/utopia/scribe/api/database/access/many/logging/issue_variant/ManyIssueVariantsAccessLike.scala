package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.util.Version
import utopia.scribe.api.database.model.logging.{IssueOccurrenceModel, IssueVariantModel}
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.FilterableView

import java.time.Instant

/**
  * A common trait for access points which target multiple issue variants or similar instances at a time
  * @author Mikko Hilpinen
  * @since 23.05.2023, v0.1
  */
trait ManyIssueVariantsAccessLike[+A, +Repr] extends ManyModelAccess[A] with Indexed with FilterableView[Repr]
{
	// COMPUTED	--------------------
	
	/**
	  * issue ids of the accessible issue variants
	  */
	def issueIds(implicit connection: Connection) = pullColumn(model.issueIdColumn).map { v => v.getInt }
	
	/**
	  * versions of the accessible issue variants
	  */
	def versions(implicit connection: Connection) = 
		pullColumn(model.versionColumn).map { v => Version(v.getString) }
	
	/**
	  * error ids of the accessible issue variants
	  */
	def errorIds(implicit connection: Connection) = pullColumn(model.errorIdColumn).flatMap { v => v.int }
	
	/**
	  * details of the accessible issue variants
	  */
	def details(implicit connection: Connection) = 
		pullColumn(model.detailsColumn)
			.map { v => v.mapIfNotEmpty { v => JsonBunny.sureMunch(v.getString).getModel } }
	
	/**
	  * creation times of the accessible issue variants
	  */
	def creationTimes(implicit connection: Connection) = pullColumn(model.createdColumn)
		.map { v => v.getInstant }
	
	def ids(implicit connection: Connection) = pullColumn(index).map { v => v.getInt }
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueVariantModel
	
	protected def occurrenceModel = IssueOccurrenceModel
	
	
	// OTHER	--------------------
	
	/**
	  * @param issueId Id of the targeted issue
	  * @param version Targeted version
	  * @return Access to that issue's variants that occurred in the specified version
	  */
	def matching(issueId: Int, version: Version) =
		filter(model.withIssueId(issueId).withVersion(version).toCondition)
	
	/**
	  * @param errorId Id of an error, or None if targeting variants that are not associated with any error
	  * @return Access to issue variants caused by the specified error (or not caused by any error)
	  */
	def causedByError(errorId: Option[Int]) = {
		val condition = errorId match {
			case Some(errorId) => model.withErrorId(errorId).toCondition
			case None => model.errorIdColumn.isNull
		}
		filter(condition)
	}
	
	/**
	  * @param threshold  A time threshold
	  * @param connection Implicit DB Connection
	  * @return Issue variants that have not occurred at all since the specified time threshold
	  */
	def findNotOccurredSince(threshold: Instant)(implicit connection: Connection) =
		findNotLinkedTo(occurrenceModel.table, Some(occurrenceModel.latestColumn > threshold))
	
	/**
	  * Updates the creation times of the targeted issue variants
	  * @param newCreated A new created to assign
	  * @return Whether any issue variant was affected
	  */
	def creationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the details of the targeted issue variants
	  * @param newDetails A new details to assign
	  * @return Whether any issue variant was affected
	  */
	def details_=(newDetails: Model)(implicit connection: Connection) = 
		putColumn(model.detailsColumn, newDetails.notEmpty.map { _.toJson })
	
	/**
	  * Updates the error ids of the targeted issue variants
	  * @param newErrorId A new error id to assign
	  * @return Whether any issue variant was affected
	  */
	def errorIds_=(newErrorId: Int)(implicit connection: Connection) = putColumn(model.errorIdColumn, 
		newErrorId)
	
	/**
	  * Updates the issue ids of the targeted issue variants
	  * @param newIssueId A new issue id to assign
	  * @return Whether any issue variant was affected
	  */
	def issueIds_=(newIssueId: Int)(implicit connection: Connection) = putColumn(model.issueIdColumn, 
		newIssueId)
	
	/**
	  * Updates the versions of the targeted issue variants
	  * @param newVersion A new version to assign
	  * @return Whether any issue variant was affected
	  */
	def versions_=(newVersion: Version)(implicit connection: Connection) = 
		putColumn(model.versionColumn, newVersion.toString)
}

