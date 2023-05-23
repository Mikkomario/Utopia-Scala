package utopia.scribe.api.database.access.single.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Version
import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.api.database.model.logging.IssueVariantModel
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.SingleChronoRowModelAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.sql.Condition

import java.time.Instant

object UniqueIssueVariantAccess
{
	// OTHER	--------------------
	
	/**
	  * @param condition Condition to apply to all requests
	  * @return An access point that applies the specified filter condition (only)
	  */
	def apply(condition: Condition): UniqueIssueVariantAccess = new _UniqueIssueVariantAccess(condition)
	
	
	// NESTED	--------------------
	
	private class _UniqueIssueVariantAccess(condition: Condition) extends UniqueIssueVariantAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return individual and distinct issue variants.
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait UniqueIssueVariantAccess 
	extends SingleChronoRowModelAccess[IssueVariant, UniqueIssueVariantAccess] 
		with DistinctModelAccess[IssueVariant, Option[IssueVariant], Value] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Id of the issue that occurred. None if no issue variant (or value) was found.
	  */
	def issueId(implicit connection: Connection) = pullColumn(model.issueIdColumn).int
	
	/**
	  * 
		The program version in which this issue (variant) occurred. None if no issue variant (or value) was found.
	  */
	def version(implicit connection: Connection) = 
		pullColumn(model.versionColumn).string.flatMap(Version.findFrom)
	
	/**
	  * Id of the error / exception that is associated 
	  * with this issue (variant). None if not applicable.. None if no issue variant (or value) was found.
	  */
	def errorId(implicit connection: Connection) = pullColumn(model.errorIdColumn).int
	
	/**
	  * Details about this case and/or setting.. None if no issue variant (or value) was found.
	  */
	def details(implicit connection: Connection) = pullColumn(model.detailsColumn).getString
	
	/**
	  * Time when this case or variant was first encountered. None if no issue variant (or value) was found.
	  */
	def created(implicit connection: Connection) = pullColumn(model.createdColumn).instant
	
	def id(implicit connection: Connection) = pullColumn(index).int
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = IssueVariantModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): UniqueIssueVariantAccess = 
		new UniqueIssueVariantAccess._UniqueIssueVariantAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted issue variants
	  * @param newCreated A new created to assign
	  * @return Whether any issue variant was affected
	  */
	def created_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(model.createdColumn, newCreated)
	
	/**
	  * Updates the details of the targeted issue variants
	  * @param newDetails A new details to assign
	  * @return Whether any issue variant was affected
	  */
	def details_=(newDetails: String)(implicit connection: Connection) = 
		putColumn(model.detailsColumn, newDetails)
	
	/**
	  * Updates the error ids of the targeted issue variants
	  * @param newErrorId A new error id to assign
	  * @return Whether any issue variant was affected
	  */
	def errorId_=(newErrorId: Int)(implicit connection: Connection) = putColumn(model.errorIdColumn, 
		newErrorId)
	
	/**
	  * Updates the issue ids of the targeted issue variants
	  * @param newIssueId A new issue id to assign
	  * @return Whether any issue variant was affected
	  */
	def issueId_=(newIssueId: Int)(implicit connection: Connection) = putColumn(model.issueIdColumn, 
		newIssueId)
	
	/**
	  * Updates the versions of the targeted issue variants
	  * @param newVersion A new version to assign
	  * @return Whether any issue variant was affected
	  */
	def version_=(newVersion: Version)(implicit connection: Connection) = 
		putColumn(model.versionColumn, newVersion.toString)
}

