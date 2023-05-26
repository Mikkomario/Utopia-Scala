package utopia.scribe.api.database.access.many.logging.issue

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.util.Version
import utopia.scribe.api.database.factory.logging.VaryingIssueFactory
import utopia.scribe.api.database.model.logging.IssueVariantModel
import utopia.scribe.core.model.combined.logging.VaryingIssue
import utopia.vault.database.Connection
import utopia.vault.sql.Condition

import java.time.Instant

object ManyVaryingIssuesAccess
{
	// NESTED	--------------------
	
	private class SubAccess(condition: Condition) extends ManyVaryingIssuesAccess
	{
		// IMPLEMENTED	--------------------
		
		override def globalCondition = Some(condition)
	}
}

/**
  * A common trait for access points that return multiple varying issues at a time
  * @author Mikko Hilpinen
  * @since 26.05.2023
  */
trait ManyVaryingIssuesAccess extends ManyIssuesAccessLike[VaryingIssue, ManyVaryingIssuesAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * issue ids of the accessible issue variants
	  */
	def variantIssueIds(implicit connection: Connection) = 
		pullColumn(variantModel.issueIdColumn).map { v => v.getInt }
	
	/**
	  * versions of the accessible issue variants
	  */
	def variantVersions(implicit connection: Connection) = 
		pullColumn(variantModel.versionColumn).map { v => Version(v.getString) }
	
	/**
	  * error ids of the accessible issue variants
	  */
	def variantErrorIds(implicit connection: Connection) = 
		pullColumn(variantModel.errorIdColumn).flatMap { v => v.int }
	
	/**
	  * details of the accessible issue variants
	  */
	def variantsDetails(implicit connection: Connection) = 
		pullColumn(variantModel.detailsColumn).flatMap { _.string }
	
	/**
	  * creation times of the accessible issue variants
	  */
	def variantCreationTimes(implicit connection: Connection) = 
		pullColumn(variantModel.createdColumn).map { v => v.getInstant }
	
	/**
	  * Model (factory) used for interacting the issue variants associated with this varying issue
	  */
	protected def variantModel = IssueVariantModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = VaryingIssueFactory
	
	override protected def self = this
	
	override def filter(filterCondition: Condition): ManyVaryingIssuesAccess = 
		new ManyVaryingIssuesAccess.SubAccess(mergeCondition(filterCondition))
	
	
	// OTHER	--------------------
	
	/**
	  * Updates the creation times of the targeted issue variants
	  * @param newCreated A new created to assign
	  * @return Whether any issue variant was affected
	  */
	def variantCreationTimes_=(newCreated: Instant)(implicit connection: Connection) = 
		putColumn(variantModel.createdColumn, newCreated)
	
	/**
	  * Updates the error ids of the targeted issue variants
	  * @param newErrorId A new error id to assign
	  * @return Whether any issue variant was affected
	  */
	def variantErrorIds_=(newErrorId: Int)(implicit connection: Connection) = 
		putColumn(variantModel.errorIdColumn, newErrorId)
	
	/**
	  * Updates the issue ids of the targeted issue variants
	  * @param newIssueId A new issue id to assign
	  * @return Whether any issue variant was affected
	  */
	def variantIssueIds_=(newIssueId: Int)(implicit connection: Connection) = 
		putColumn(variantModel.issueIdColumn, newIssueId)
	
	/**
	  * Updates the versions of the targeted issue variants
	  * @param newVersion A new version to assign
	  * @return Whether any issue variant was affected
	  */
	def variantVersions_=(newVersion: Version)(implicit connection: Connection) = 
		putColumn(variantModel.versionColumn, newVersion.toString)
	
	/**
	  * Updates the details of the targeted issue variants
	  * @param newDetails A new details to assign
	  * @return Whether any issue variant was affected
	  */
	def variantsDetails_=(newDetails: String)(implicit connection: Connection) = 
		putColumn(variantModel.detailsColumn, newDetails)
}

