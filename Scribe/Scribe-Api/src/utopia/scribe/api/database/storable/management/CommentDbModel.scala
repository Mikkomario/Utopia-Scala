package utopia.scribe.api.database.storable.management

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.core.model.factory.management.CommentFactory
import utopia.scribe.core.model.partial.management.CommentData
import utopia.scribe.core.model.stored.management.Comment
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.HasIdProperty
import utopia.vault.nosql.storable.StorableFactory
import utopia.vault.store.{FromIdFactory, HasId}

import java.time.Instant

/**
  * Used for constructing CommentDbModel instances and for inserting comments to the database
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
object CommentDbModel 
	extends StorableFactory[CommentDbModel, Comment, CommentData] with FromIdFactory[Int, CommentDbModel] 
		with HasIdProperty with CommentFactory[CommentDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with issue variant ids
	  */
	lazy val issueVariantId = property("issueVariantId")
	
	/**
	  * Database property used for interacting with texts
	  */
	lazy val text = property("text")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = ScribeTables.comment
	
	override def apply(data: CommentData): CommentDbModel = 
		apply(None, Some(data.issueVariantId), data.text, Some(data.created))
	
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	override def withIssueVariantId(issueVariantId: Int) = apply(issueVariantId = Some(issueVariantId))
	
	override def withText(text: String) = apply(text = text)
	
	override protected def complete(id: Value, data: CommentData) = Comment(id.getInt, data)
}

/**
  * Used for interacting with Comments in the database
  * @param id comment database id
  * @author Mikko Hilpinen
  * @since 26.08.2025, v1.2
  */
case class CommentDbModel(id: Option[Int] = None, issueVariantId: Option[Int] = None, text: String = "", 
	created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, CommentDbModel] 
		with CommentFactory[CommentDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val valueProperties: Seq[(String, Value)] = 
		Vector(CommentDbModel.id.name -> id, CommentDbModel.issueVariantId.name -> issueVariantId, 
			CommentDbModel.text.name -> text, CommentDbModel.created.name -> created)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = CommentDbModel.table
	
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	override def withIssueVariantId(issueVariantId: Int) = copy(issueVariantId = Some(issueVariantId))
	
	override def withText(text: String) = copy(text = text)
}

