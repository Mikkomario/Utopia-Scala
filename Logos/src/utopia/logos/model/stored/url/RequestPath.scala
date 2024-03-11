package utopia.logos.model.stored.url

import utopia.logos.database.access.single.url.request_path.DbSingleRequestPath
import utopia.logos.model.partial.url.RequestPathData
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a request path that has already been stored in the database
  * @param id id of this request path in the database
  * @param data Wrapped request path data
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class RequestPath(id: Int, data: RequestPathData) extends StoredModelConvertible[RequestPathData]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this request path in the database
	  */
	def access = DbSingleRequestPath(id)
}

