package utopia.logos.database.access.single.url.domain

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.logos.model.stored.url.Domain

/**
  * An access point to individual domains, based on their id
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class DbSingleDomain(id: Int) extends UniqueDomainAccess with SingleIntIdModelAccess[Domain]

