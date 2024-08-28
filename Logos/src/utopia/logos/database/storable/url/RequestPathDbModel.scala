package utopia.logos.database.storable.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.LogosTables
import utopia.logos.model.factory.url.RequestPathFactory
import utopia.logos.model.partial.url.RequestPathData
import utopia.logos.model.stored.url.RequestPath
import utopia.vault.model.immutable.{DbPropertyDeclaration, Storable}
import utopia.vault.model.template.{FromIdFactory, HasId, HasIdProperty}
import utopia.vault.nosql.storable.StorableFactory

import java.time.Instant

/**
  * Used for constructing RequestPathDbModel instances and for inserting request paths to the database
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object RequestPathDbModel 
	extends StorableFactory[RequestPathDbModel, RequestPath, RequestPathData] 
		with FromIdFactory[Int, RequestPathDbModel] with HasIdProperty 
		with RequestPathFactory[RequestPathDbModel]
{
	// ATTRIBUTES	--------------------
	
	override lazy val id = DbPropertyDeclaration("id", index)
	
	/**
	  * Database property used for interacting with domain ids
	  */
	lazy val domainId = property("domainId")
	
	/**
	  * Database property used for interacting with paths
	  */
	lazy val path = property("path")
	
	/**
	  * Database property used for interacting with creation times
	  */
	lazy val created = property("created")
	
	
	// IMPLEMENTED	--------------------
	
	override def table = LogosTables.requestPath
	
	override def apply(data: RequestPathData) = apply(None, Some(data.domainId), data.path, 
		Some(data.created))
	
	/**
	  * @param created Time when this request path was added to the database
	  * @return A model containing only the specified created
	  */
	override def withCreated(created: Instant) = apply(created = Some(created))
	
	/**
	  * @param domainId Id of the domain part of this url
	  * @return A model containing only the specified domain id
	  */
	override def withDomainId(domainId: Int) = apply(domainId = Some(domainId))
	
	override def withId(id: Int) = apply(id = Some(id))
	
	/**
	  * @param path Part of this url that comes after the domain part. Doesn't include any query parameters, 
	  * nor the initial forward slash.
	  * @return A model containing only the specified path
	  */
	override def withPath(path: String) = apply(path = path)
	
	override protected def complete(id: Value, data: RequestPathData) = RequestPath(id.getInt, data)
}

/**
  * Used for interacting with RequestPaths in the database
  * @param id request path database id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class RequestPathDbModel(id: Option[Int] = None, domainId: Option[Int] = None, path: String = "", 
	created: Option[Instant] = None) 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, RequestPathDbModel] 
		with RequestPathFactory[RequestPathDbModel]
{
	// IMPLEMENTED	--------------------
	
	override def table = RequestPathDbModel.table
	
	override def valueProperties = 
		Vector(RequestPathDbModel.id.name -> id, RequestPathDbModel.domainId.name -> domainId, 
			RequestPathDbModel.path.name -> path, RequestPathDbModel.created.name -> created)
	
	/**
	  * @param created Time when this request path was added to the database
	  * @return A new copy of this model with the specified created
	  */
	override def withCreated(created: Instant) = copy(created = Some(created))
	
	/**
	  * @param domainId Id of the domain part of this url
	  * @return A new copy of this model with the specified domain id
	  */
	override def withDomainId(domainId: Int) = copy(domainId = Some(domainId))
	
	override def withId(id: Int) = copy(id = Some(id))
	
	/**
	  * @param path Part of this url that comes after the domain part. Doesn't include any query parameters, 
	  * nor the initial forward slash.
	  * @return A new copy of this model with the specified path
	  */
	override def withPath(path: String) = copy(path = path)
}

