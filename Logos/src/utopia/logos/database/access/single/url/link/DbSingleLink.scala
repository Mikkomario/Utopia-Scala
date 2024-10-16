package utopia.logos.database.access.single.url.link

import utopia.logos.model.stored.url.StoredLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual links, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DbSingleLink(id: Int) extends UniqueLinkAccess with SingleIntIdModelAccess[StoredLink]

