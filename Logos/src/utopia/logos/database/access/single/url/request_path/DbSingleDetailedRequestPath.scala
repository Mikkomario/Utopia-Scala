package utopia.logos.database.access.single.url.request_path

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.logos.model.combined.url.DetailedRequestPath

/**
  * An access point to individual detailed request paths, based on their request path id
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class DbSingleDetailedRequestPath(id: Int) 
	extends UniqueDetailedRequestPathAccess with SingleIntIdModelAccess[DetailedRequestPath]

