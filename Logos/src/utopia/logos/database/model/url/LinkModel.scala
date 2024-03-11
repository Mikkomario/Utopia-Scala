package utopia.logos.database.model.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.{Model, Value}
import utopia.logos.database.factory.url.LinkFactory
import utopia.logos.model.partial.url.LinkData
import utopia.logos.model.stored.url.Link
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing LinkModel instances and for inserting links to the database
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object LinkModel extends DataInserter[LinkModel, Link, LinkData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains link request path id
	  */
	val requestPathIdAttName = "requestPathId"
	
	/**
	  * Name of the property that contains link query parameters
	  */
	val queryParametersAttName = "queryParameters"
	
	/**
	  * Name of the property that contains link created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains link request path id
	  */
	def requestPathIdColumn = table(requestPathIdAttName)
	
	/**
	  * Column that contains link query parameters
	  */
	def queryParametersColumn = table(queryParametersAttName)
	
	/**
	  * Column that contains link created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = LinkFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: LinkData) = 
		apply(None, Some(data.requestPathId), data.queryParameters, Some(data.created))
	
	override protected def complete(id: Value, data: LinkData) = Link(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this link was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param id A link id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param queryParameters Specified request parameters in model format
	  * @return A model containing only the specified query parameters
	  */
	def withQueryParameters(queryParameters: Model) = apply(queryParameters = queryParameters)
	
	/**
	  * @param requestPathId Id of the targeted internet address, including the specific sub-path
	  * @return A model containing only the specified request path id
	  */
	def withRequestPathId(requestPathId: Int) = apply(requestPathId = Some(requestPathId))
}

/**
  * Used for interacting with Links in the database
  * @param id link database id
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class LinkModel(id: Option[Int] = None, requestPathId: Option[Int] = None, 
	queryParameters: Model = Model.empty, created: Option[Instant] = None) 
	extends StorableWithFactory[Link]
{
	// IMPLEMENTED	--------------------
	
	override def factory = LinkModel.factory
	
	override def valueProperties = {
		import LinkModel._
		Vector("id" -> id, requestPathIdAttName -> requestPathId, 
			queryParametersAttName -> queryParameters.notEmpty.map { _.toJson }, createdAttName -> created)
	}
	
	
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
	def withRequestPathId(requestPathId: Int) = copy(requestPathId = Some(requestPathId))
}

