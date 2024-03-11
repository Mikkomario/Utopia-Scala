package utopia.logos.database.model.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.RequestPathFactory
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.nosql.storable.DataInserter

import java.time.Instant

/**
  * Used for constructing RequestPathModel instances and for inserting request paths to the database
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
object RequestPathModel extends DataInserter[RequestPathModel, RequestPath, RequestPathData]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Name of the property that contains request path domain id
	  */
	val domainIdAttName = "domainId"
	
	/**
	  * Name of the property that contains request path path
	  */
	val pathAttName = "path"
	
	/**
	  * Name of the property that contains request path created
	  */
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * Column that contains request path domain id
	  */
	def domainIdColumn = table(domainIdAttName)
	
	/**
	  * Column that contains request path path
	  */
	def pathColumn = table(pathAttName)
	
	/**
	  * Column that contains request path created
	  */
	def createdColumn = table(createdAttName)
	
	/**
	  * The factory object used by this model type
	  */
	def factory = RequestPathFactory
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: RequestPathData) = apply(None, Some(data.domainId), data.path, 
		Some(data.created))
	
	override protected def complete(id: Value, data: RequestPathData) = RequestPath(id.getInt, data)
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this request path was added to the database
	  * @return A model containing only the specified created
	  */
	def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param domainId Id of the domain part of this url
	  * @return A model containing only the specified domain id
	  */
	def withDomainId(domainId: Int) = apply(domainId = Some(domainId))
	
	/**
	  * @param id A request path id
	  * @return A model with that id
	  */
	def withId(id: Int) = apply(Some(id))
	
	/**
	  * @param path Part of this url that comes after the domain part. Doesn't include any query parameters, 
	  * nor the initial forward slash.
	  * @return A model containing only the specified path
	  */
	def withPath(path: String) = apply(path = path)
}

/**
  * Used for interacting with RequestPaths in the database
  * @param id request path database id
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class RequestPathModel(id: Option[Int] = None, domainId: Option[Int] = None, path: String = "", 
	created: Option[Instant] = None) 
	extends StorableWithFactory[RequestPath]
{
	// IMPLEMENTED	--------------------
	
	override def factory = RequestPathModel.factory
	
	override def valueProperties = {
		import RequestPathModel._
		Vector("id" -> id, domainIdAttName -> domainId, pathAttName -> path, createdAttName -> created)
	}
	
	
	// OTHER	--------------------
	
	/**
	  * @param created Time when this request path was added to the database
	  * @return A new copy of this model with the specified created
	  */
	def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param domainId Id of the domain part of this url
	  * @return A new copy of this model with the specified domain id
	  */
	def withDomainId(domainId: Int) = copy(domainId = Some(domainId))
	
	/**
	  * @param path Part of this url that comes after the domain part. Doesn't include any query parameters, 
	  * nor the initial forward slash.
	  * @return A new copy of this model with the specified path
	  */
	def withPath(path: String) = copy(path = path)
}

