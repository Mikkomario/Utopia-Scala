package utopia.logos.database.access.single.url.path

import utopia.logos.model.stored.url.RequestPath
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual request paths, based on their id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleRequestPath(id: Int) 
	extends UniqueRequestPathAccess with SingleIntIdModelAccess[RequestPath]

