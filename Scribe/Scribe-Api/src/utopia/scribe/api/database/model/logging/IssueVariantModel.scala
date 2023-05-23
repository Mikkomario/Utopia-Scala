package utopia.scribe.api.database.model.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.util.Version
import utopia.scribe.api.database.factory.logging.IssueVariantFactory
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing IssueVariantModel instances and for inserting issue variants to the database
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
object IssueVariantModel extends DataInserter[IssueVariantModel, IssueVariant, IssueVariantData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains issue variant issue id
	  */
	val issueIdAttName = "issueId"
	
	/**
	  * Name of the property that contains issue variant version
	  */
	val versionAttName = "version"
	
	/**
	  * Name of the property that contains issue variant error id
	  */
	val errorIdAttName = "errorId"
	
	/**
	  * Name of the property that contains issue variant details
	  */
	val detailsAttName = "details"
	
	/**
	  * Name of the property that contains issue variant created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains issue variant issue id
	  */
	def issueIdColumn = table(issueIdAttName)
	
	/**
	  * Column that contains issue variant version
	  */
	def versionColumn = table(versionAttName)
	
	/**
	  * Column that contains issue variant error id
	  */
	def errorIdColumn = table(errorIdAttName)
	
	/**
	  * Column that contains issue variant details
	  */
	def detailsColumn = table(detailsAttName)
	
	/**
	  * Column that contains issue variant created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = IssueVariantFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: IssueVariantData) = 
		apply(None, Some(data.issueId), Some(data.version), data.errorId, data.details, Some(data.created))
	
	override protected def complete(id: Value, data: IssueVariantData) = IssueVariant(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this case or variant was first encountered
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param details Details about this case and/or setting.
	  * @return A model containing only the specified details
	  */
	def withDetails(details: String) = apply(details = details)
	
	/**
	  * @param errorId Id of the error / exception that is associated 
	  * with this issue (variant). None if not applicable.
	  * @return A model containing only the specified error id
	  */
	def withErrorId(errorId: Int) = apply(errorId = Some(errorId))
	
	/**
	  * @param id A issue variant id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param issueId Id of the issue that occurred
	  * @return A model containing only the specified issue id
	  */
	def withIssueId(issueId: Int) = apply(issueId = Some(issueId))
	
	/**
	  * @param version The program version in which this issue (variant) occurred
	  * @return A model containing only the specified version
	  */
	def withVersion(version: Version) = apply(version = Some(version))
}

/**
  * Used for interacting with IssueVariants in the database
  * @param id issue variant database id
  * @author Mikko Hilpinen
  * @since 22.05.2023, v0.1
  */
case class IssueVariantModel(id: Option[Int] = None, issueId: Option[Int] = None, 
	version: Option[Version] = None, errorId: Option[Int] = None, details: String = "", 
	created: Option[Instant] = None) 
	extends StorableWithFactory[IssueVariant]
{
	// IMPLEMENTED	--------------------
	
	override def factory = IssueVariantModel.factory
	
	override def valueProperties = {
		import IssueVariantModel._
		Vector("id" -> id, issueIdAttName -> issueId, versionAttName -> version.map { _.toString }, 
			errorIdAttName -> errorId, detailsAttName -> details, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this case or variant was first encountered
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param details Details about this case and/or setting.
	  * @return A new copy of this model with the specified details
	  */
	def withDetails(details: String) = copy(details = details)
	
	/**
	  * @param errorId Id of the error / exception that is associated 
	  * with this issue (variant). None if not applicable.
	  * @return A new copy of this model with the specified error id
	  */
	def withErrorId(errorId: Int) = copy(errorId = Some(errorId))
	
	/**
	  * @param issueId Id of the issue that occurred
	  * @return A new copy of this model with the specified issue id
	  */
	def withIssueId(issueId: Int) = copy(issueId = Some(issueId))
	
	/**
	  * @param version The program version in which this issue (variant) occurred
	  * @return A new copy of this model with the specified version
	  */
	def withVersion(version: Version) = copy(version = Some(version))
}

