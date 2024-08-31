package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.logos.database.factory.url.LinkDbFactory
import utopia.logos.model.factory.url.LinkFactory
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.StoredLink
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing LinkModel instances and for inserting links to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with LinkDbModel", "v0.3")
object LinkModel
	extends StorableFactory[LinkModel, StoredLink, LinkData] with LinkFactory[LinkModel] with FromIdFactory[Int, LinkModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val requestPathId = property("requestPathId")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val queryParameters = property("queryParameters")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val created = property("created")
	
	/**
	  * Name of the property that contains link request path id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val requestPathIdAttName = "requestPathId"
	/**
	  * Name of the property that contains link query parameters
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val queryParametersAttName = "queryParameters"
	/**
	  * Name of the property that contains link created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = LinkDbFactory
	
	/**
	  * Column that contains link request path id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def requestPathIdColumn = table(requestPathIdAttName)
	/**
	  * Column that contains link query parameters
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def queryParametersColumn = table(queryParametersAttName)
	/**
	  * Column that contains link created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def createdColumn = table(createdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: LinkData) = 
		apply(None, Some(data.pathId), data.queryParameters, Some(data.created))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
	override protected def complete(id: Value, data: LinkData) = StoredLink(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this link was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param queryParameters Specified request parameters in model format
	  * @return A model containing only the specified query parameters
	  */
	def withQueryParameters(queryParameters: Model) = apply(queryParameters = queryParameters)
	
	/**
	  * @param requestPathId Id of the targeted internet address, including the specific sub-path
	  * @return A model containing only the specified request path id
	  */
	def withPathId(requestPathId: Int) = apply(requestPathId = Some(requestPathId))
}

/**
  * Used for interacting with Links in the database
  * @param id link database id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
@deprecated("Replaced with LinkDbModel", "v0.3")
case class LinkModel(id: Option[Int] = None, requestPathId: Option[Int] = None, 
	queryParameters: Model = Model.empty, created: Option[Instant] = None) 
	extends StorableWithFactory[StoredLink] with LinkFactory[LinkModel] with FromIdFactory[Int, LinkModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkModel.factory
	
	override def valueProperties =
		Vector("id" -> id, LinkModel.requestPathId.name -> requestPathId, 
			LinkModel.queryParameters.name -> queryParameters.notEmpty.map { _.toJson }, 
			LinkModel.created.name -> created)
	
	override def withId(id: Int): LinkModel = copy(id = Some(id))
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this link was added to the database
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param queryParameters Specified request parameters in model format
	  * @return A new copy of this model with the specified query parameters
	  */
	def withQueryParameters(queryParameters: Model) = copy(queryParameters = queryParameters)
	
	/**
	  * @param requestPathId Id of the targeted internet address, including the specific sub-path
	  * @return A new copy of this model with the specified request path id
	  */
	def withPathId(requestPathId: Int) = copy(requestPathId = Some(requestPathId))
}

