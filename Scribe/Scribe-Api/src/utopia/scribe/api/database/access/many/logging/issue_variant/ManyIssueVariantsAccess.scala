package utopia.scribe.api.database.access.many.logging.issue_variant

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.Version
import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.api.database.model.logging.IssueVariantModel
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.ChronoRowFactoryView
import utopia.vault.sql.Condition

import java.time.Instant

object ManyIssueVariantsAccess
{
	// NESTED	--------------------
	
	private class ManyIssueVariantsSubView(condition: Condition) extends ManyIssueVariantsAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points which target multiple issue variants at a time
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
trait ManyIssueVariantsAccess 
	extends ManyRowModelAccess[IssueVariant] with ChronoRowFactoryView[IssueVariant, ManyIssueVariantsAccess] 
		with Indexed
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
	def details(implicit connection: Connection) = pullColumn(model.detailsColumn).flatMap { _.string }
	
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
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyIssueVariantsAccess = 
		new ManyIssueVariantsAccess.ManyIssueVariantsSubView(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
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
	def details_=(newDetails: String)(implicit connection: Connection) = 
		putColumn(model.detailsColumn, newDetails)
	
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

