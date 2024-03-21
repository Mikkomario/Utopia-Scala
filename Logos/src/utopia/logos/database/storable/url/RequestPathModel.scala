package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.factory.url.RequestPathDbFactory
import utopia.logos.model.factory.url.RequestPathFactory
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.model.immutable.StorableWithFactory
import utopia.vault.model.template.FromIdFactory
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing RequestPathModel instances and for inserting request paths to the database
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
object RequestPathModel 
	extends StorableFactory[RequestPathModel, RequestPath, RequestPathData] 
		with RequestPathFactory[RequestPathModel] with FromIdFactory[Int, RequestPathModel]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val domainId = property("domainId")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val path = property("path")
	/**
	  * Property that contains ${ classToWrite.name.doc } ${ prop.name.doc }
	  */
	lazy val created = property("created")
	
	/**
	  * Name of the property that contains request path domain id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val domainIdAttName = "domainId"
	/**
	  * Name of the property that contains request path path
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val pathAttName = "path"
	/**
	  * Name of the property that contains request path created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	val createdAttName = "created"
	
	
	// COMPUTED	--------------------
	
	/**
	  * The factory object used by this model type
	  */
	def factory = RequestPathDbFactory
	
	/**
	  * Column that contains request path domain id
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def domainIdColumn = table(domainIdAttName)
	/**
	  * Column that contains request path path
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def pathColumn = table(pathAttName)
	/**
	  * Column that contains request path created
	  */
	@deprecated("Deprecated for removal", "v1.0")
	def createdColumn = table(createdAttName)
	
	
	// IMPLEMENTED	--------------------
	
	override def table = factory.table
	
	override def apply(data: RequestPathData) = apply(None, Some(data.domainId), data.path, 
		Some(data.created))
	
	/**
	  * @return A model with that id
	  */
	override def withId(id: Int) = apply(Some(id))
	
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
  * @since 20.03.2024, v1.0
  */
case class RequestPathModel(id: Option[Int] = None, domainId: Option[Int] = None, path: String = "", 
	created: Option[Instant] = None) 
	extends StorableWithFactory[RequestPath] with RequestPathFactory[RequestPathModel]
		with FromIdFactory[Int, RequestPathModel]
{
	// IMPLEMENTED	--------------------
	
	override def factory = RequestPathModel.factory
	
	override def valueProperties =
		Vector("id" -> id, RequestPathModel.domainId.name -> domainId, RequestPathModel.path.name -> path, 
			RequestPathModel.created.name -> created)
	
	override def withId(id: Int): RequestPathModel = copy(id = Some(id))
	
	
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

