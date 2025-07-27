package utopia.scribe.api.database.storable.logging

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.flow.util.Version
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.logging.IssueVariantFactory
import utopia.scribe.core.model.partial.logging.IssueVariantData
import utopia.scribe.core.model.stored.logging.IssueVariant
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing IssueVariantDbModel instances and for inserting issue variants to the 
  * database
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
object IssueVariantDbModel 
	extends StorableFactory[IssueVariantDbModel, IssueVariant, IssueVariantData] 
		with FromIdFactory[Int, IssueVariantDbModel] with HasIdProperty 
		with IssueVariantFactory[IssueVariantDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with issue ids
	  */
	lazy val issueId = property("issueId")
	
	/**
	  * Database property used for interacting with versions
	  */
	lazy val version = property("version")
	
	/**
	  * Database property used for interacting with error ids
	  */
	lazy val errorId = property("errorId")
	
	/**
	  * Database property used for interacting with details
	  */
	lazy val details = property("details")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.issueVariant
	
	override def apply(data: IssueVariantData): IssueVariantDbModel = 
		apply(None, Some(data.issueId), Some(data.version), data.errorId, data.details, Some(data.created))
	
	/**
	  * @param created Time when this case or variant was first encountered
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param details Details about this case and/or setting.
	  * @return A model containing only the specified details
	  */
	override def withDetails(details: Model) = apply(details = details)
	
	/**
	  * @param errorId Id of the error / exception that is associated with this issue (variant). None 
	  *                if not applicable.
	  * @return A model containing only the specified error id
	  */
	override def withErrorId(errorId: Int) = apply(errorId = Some(errorId))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param issueId Id of the issue that occurred
	  * @return A model containing only the specified issue id
	  */
	override def withIssueId(issueId: Int) = apply(issueId = Some(issueId))
	
	/**
	  * @param version The program version in which this issue (variant) occurred
	  * @return A model containing only the specified version
	  */
	override def withVersion(version: Version) = apply(version = Some(version))
	
	override protected def complete(id: Value, data: IssueVariantData) = IssueVariant(id.getInt, data)
}

/**
  * Used for interacting with IssueVariants in the database
  * @param id issue variant database id
  * @author Mikko Hilpinen
  * @since 27.07.2025, v0.1
  */
case class IssueVariantDbModel(id: Option[Int] = None, issueId: Option[Int] = None, 
	version: Option[Version] = None, errorId: Option[Int] = None, details: Model = Model.empty, 
	created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, IssueVariantDbModel] 
		with IssueVariantFactory[IssueVariantDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(IssueVariantDbModel.id.name -> id, IssueVariantDbModel.issueId.name -> issueId, 
			IssueVariantDbModel.version.name -> version.map { _.toString }, 
			IssueVariantDbModel.errorId.name -> errorId, 
			IssueVariantDbModel.details.name -> details.notEmpty.map { _.toJson }, 
			IssueVariantDbModel.created.name -> created)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = IssueVariantDbModel.table
	
	/**
	  * @param created Time when this case or variant was first encountered
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param details Details about this case and/or setting.
	  * @return A new copy of this model with the specified details
	  */
	override def withDetails(details: Model) = copy(details = details)
	
	/**
	  * @param errorId Id of the error / exception that is associated with this issue (variant). None 
	  *                if not applicable.
	  * @return A new copy of this model with the specified error id
	  */
	override def withErrorId(errorId: Int) = copy(errorId = Some(errorId))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param issueId Id of the issue that occurred
	  * @return A new copy of this model with the specified issue id
	  */
	override def withIssueId(issueId: Int) = copy(issueId = Some(issueId))
	
	/**
	  * @param version The program version in which this issue (variant) occurred
	  * @return A new copy of this model with the specified version
	  */
	override def withVersion(version: Version) = copy(version = Some(version))
}

