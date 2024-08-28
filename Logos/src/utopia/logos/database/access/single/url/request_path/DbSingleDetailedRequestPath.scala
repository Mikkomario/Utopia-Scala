package utopia.logos.database.access.single.url.request_path

import utopia.logos.model.combined.url.DetailedRequestPath
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual detailed request paths, based on their request path id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
@deprecated("Replaced with a new version", "v0.3")
case class DbSingleDetailedRequestPath(id: Int) 
	extends UniqueDetailedRequestPathAccess with SingleIntIdModelAccess[DetailedRequestPath]

