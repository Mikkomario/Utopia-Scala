package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.logos.database.LogosTables
import utopia.logos.model.factory.url.LinkFactory
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{FromIdFactory, HasId, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing LinkDbModel instances and for inserting links to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object LinkDbModel 
	extends StorableFactory[LinkDbModel, StoredLink, LinkData] with FromIdFactory[Int, LinkDbModel]
		with HasIdProperty with LinkFactory[LinkDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with path ids
	  */
	lazy val pathId = property("pathId")
	
	/**
	  * Database property used for interacting with query parameters
	  */
	lazy val queryParameters = property("queryParameters")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = LogosTables.link
	
	override def apply(data: LinkData) = apply(None, Some(data.pathId), data.queryParameters, 
		Some(data.created))
	
	/**
	  * @param created Time when this link was added to the database
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param pathId Id of the targeted internet address, including the specific sub-path
	  * @return A model containing only the specified path id
	  */
	override def withPathId(pathId: Int) = apply(pathId = Some(pathId))
	
	/**
	  * @param queryParameters Specified request parameters in model format
	  * @return A model containing only the specified query parameters
	  */
	override def withQueryParameters(queryParameters: Model) = apply(queryParameters = queryParameters)
	
	override protected def complete(id: Value, data: LinkData) = StoredLink(id.getInt, data)
}

/**
  * Used for interacting with Links in the database
  * @param id link database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class LinkDbModel(id: Option[Int] = None, pathId: Option[Int] = None, 
	queryParameters: Model = Model.empty, created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, LinkDbModel] 
		with LinkFactory[LinkDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def table = LinkDbModel.table
	
	override def valueProperties = 
		Vector(LinkDbModel.id.name -> id, LinkDbModel.pathId.name -> pathId, 
			LinkDbModel.queryParameters.name -> queryParameters.notEmpty.map { _.toJson }, 
			LinkDbModel.created.name -> created)
	
	/**
	  * @param created Time when this link was added to the database
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param pathId Id of the targeted internet address, including the specific sub-path
	  * @return A new copy of this model with the specified path id
	  */
	override def withPathId(pathId: Int) = copy(pathId = Some(pathId))
	
	/**
	  * @param queryParameters Specified request parameters in model format
	  * @return A new copy of this model with the specified query parameters
	  */
	override def withQueryParameters(queryParameters: Model) = copy(queryParameters = queryParameters)
}

