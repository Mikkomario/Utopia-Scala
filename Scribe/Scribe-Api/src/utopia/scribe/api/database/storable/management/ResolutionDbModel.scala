package utopia.scribe.api.database.storable.management

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Now
import utopia.flow.util.Version
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.management.ResolutionFactory
import utopia.scribe.core.model.partial.management.ResolutionData
import utopia.scribe.core.model.stored.management.Resolution
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable, TableColumn}
import utopia.vault.model.template.{DeprecatesAfter, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.sql.Condition
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing ResolutionDbModel instances and for inserting resolutions to the 
  * database
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
object ResolutionDbModel 
	extends StorableFactory[ResolutionDbModel, Resolution, ResolutionData] 
		with FromIdFactory[Int, ResolutionDbModel] with HasIdProperty 
		with ResolutionFactory[ResolutionDbModel] with DeprecatesAfter
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	/**
	  * Database property used for interacting with resolved issue ids
	  */
	lazy val resolvedIssueId = property("resolvedIssueId")
	/**
	  * Database property used for interacting with comment ids
	  */
	lazy val commentId = property("commentId")
	/**
	  * Database property used for interacting with version thresholds
	  */
	lazy val versionThreshold = property("versionThreshold")
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	/**
	  * Database property used for interacting with deprecate at
	  */
	lazy val deprecates = property("deprecates")
	/**
	  * Database property used for interacting with silence
	  */
	lazy val silences = property("silences")
	/**
	  * Database property used for interacting with notify
	  */
	lazy val notifies = property("notifies")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.resolution
	
	override def deprecationColumn: TableColumn = deprecates
	override def activeCondition: Condition = {
		val col = deprecationColumn
		col.isNull || col > Now
	}
	
	override def apply(data: ResolutionData): ResolutionDbModel =
		apply(None, Some(data.resolvedIssueId), data.commentId, 
			data.versionThreshold match {
				case Some(version) => version.toString
				case None => ""
			},
			Some(data.created), data.deprecates, Some(data.silences), Some(data.notifies))
	
	override def withCommentId(commentId: Int) = apply(commentId = Some(commentId))
	override def withCreated(created: Instant) = apply(created = Some(created))
	override def withDeprecates(deprecates: Instant) = apply(deprecates = Some(deprecates))
	override def withId(id: Int) = apply(id = Some(id))
	override def withNotifies(notifies: Boolean) = apply(notifies = Some(notifies))
	override def withResolvedIssueId(resolvedIssueId: Int) = apply(resolvedIssueId = Some(resolvedIssueId))
	override def withSilences(silences: Boolean) = apply(silences = Some(silences))
	override def withVersionThreshold(versionThreshold: Version) = 
		apply(versionThreshold = versionThreshold.toString)
	
	override protected def complete(id: Value, data: ResolutionData) = Resolution(id.getInt, data)
}

/**
  * Used for interacting with Resolutions in the database
  * @param id resolution database id
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class ResolutionDbModel(id: Option[Int] = None, resolvedIssueId: Option[Int] = None, commentId: Option[Int] = None,
                             versionThreshold: String = "", created: Option[Instant] = None,
                             deprecates: Option[Instant] = None, silences: Option[Boolean] = None,
                             notifies: Option[Boolean] = None)
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, ResolutionDbModel] 
		with ResolutionFactory[ResolutionDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(ResolutionDbModel.id.name -> id, ResolutionDbModel.resolvedIssueId.name -> resolvedIssueId, 
			ResolutionDbModel.commentId.name -> commentId, 
			ResolutionDbModel.versionThreshold.name -> versionThreshold, 
			ResolutionDbModel.created.name -> created, ResolutionDbModel.deprecates.name -> deprecates, 
			ResolutionDbModel.silences.name -> silences, ResolutionDbModel.notifies.name -> notifies)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ResolutionDbModel.table
	
	override def withCommentId(commentId: Int) = copy(commentId = Some(commentId))
	override def withCreated(created: Instant) = copy(created = Some(created))
	override def withDeprecates(deprecates: Instant) = copy(deprecates = Some(deprecates))
	override def withId(id: Int) = copy(id = Some(id))
	override def withNotifies(notifies: Boolean) = copy(notifies = Some(notifies))
	override def withResolvedIssueId(resolvedIssueId: Int) = copy(resolvedIssueId = Some(resolvedIssueId))
	override def withSilences(silences: Boolean) = copy(silences = Some(silences))
	override def withVersionThreshold(versionThreshold: Version) = 
		copy(versionThreshold = versionThreshold.toString)
}

