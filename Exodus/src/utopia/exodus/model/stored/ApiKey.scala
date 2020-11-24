package utopia.exodus.model.stored

import utopia.exodus.model.partial.ApiKeyData
import utopia.metropolis.model.stored.Stored

/**
  * Represents a stored means of authentication via a standard api key
  * @author Mikko Hilpinen
  * @since 24.11.2020, v1
  */
case class ApiKey(id: Int, data: ApiKeyData) extends Stored[ApiKeyData]
