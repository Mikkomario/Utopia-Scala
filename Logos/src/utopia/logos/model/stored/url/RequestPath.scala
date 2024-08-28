package utopia.logos.model.stored.url

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.access.single.url.path.DbSingleRequestPath
import utopia.logos.model.factory.url.{RequestPathFactory, RequestPathFactoryWrapper}
import utopia.logos.model.partial.url.RequestPathData
import utopia.vault.model.template.{FromIdFactory, StoredFromModelFactory, StoredModelConvertible}

import java.time.Instant

object RequestPath extends StoredFromModelFactory[RequestPathData, RequestPath]
{
	// IMPLEMENTED	--------------------
	
	override def dataFactory = RequestPathData
	
	override protected def complete(model: AnyModel, data: RequestPathData) = 
		model("id").tryInt.map { apply(_, data) }
}

/**
  * Represents a request path that has already been stored in the database
  * @param id id of this request path in the database
  * @param data Wrapped request path data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class RequestPath(id: Int, data: RequestPathData) 
	extends StoredModelConvertible[RequestPathData] with FromIdFactory[Int, RequestPath] 
		with RequestPathFactoryWrapper[RequestPathData, RequestPath]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this request path in the database
	  */
	def access = DbSingleRequestPath(id)
	
	
	// IMPLEMENTED	--------------------
	
	override protected def wrappedFactory = data
	
	override def withId(id: Int) = copy(id = id)
	
	override protected def wrap(data: RequestPathData) = copy(data = data)
}

