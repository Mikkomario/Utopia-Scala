package utopia.logos.model.stored.url

import utopia.logos.database.access.single.url.request_path.DbSingleRequestPath
import utopia.logos.model.factory.url.RequestPathFactory
import utopia.logos.model.partial.url.RequestPathData
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

import java.time.Instant

/**
  * Represents a request path that has already been stored in the database
  * @param id id of this request path in the database
  * @param data Wrapped request path data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class RequestPath(id: Int, data: RequestPathData) 
	extends StoredModelConvertible[RequestPathData] with RequestPathFactory[RequestPath] 
		with FromIdFactory[Int, RequestPath]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this request path in the database
	  */
	def access = DbSingleRequestPath(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withCreated(created: Instant) = copy(data = data.withCreated(created))
	
	override def withDomainId(domainId: Int) = copy(data = data.withDomainId(domainId))
	
	override def withId(id: Int) = copy(id = id)
	
	override def withPath(path: String) = copy(data = data.withPath(path))
}

