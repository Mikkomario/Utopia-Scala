package utopia.logos.database.access.single.url.path

import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual detailed request paths, based on their request path id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleDetailedRequestPath(id: Int) 
	extends UniqueDetailedRequestPathAccess with SingleIntIdModelAccess[DetailedRequestPath]

