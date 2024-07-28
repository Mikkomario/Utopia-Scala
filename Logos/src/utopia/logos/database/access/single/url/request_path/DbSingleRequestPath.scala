package utopia.logos.database.access.single.url.request_path

import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual request paths, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DbSingleRequestPath(id: Int) 
	extends UniqueRequestPathAccess with SingleIntIdModelAccess[RequestPath]

