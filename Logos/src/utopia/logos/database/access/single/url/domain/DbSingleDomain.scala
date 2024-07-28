package utopia.logos.database.access.single.url.domain

import utopia.logos.model.stored.url.Domain
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual domains, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DbSingleDomain(id: Int) extends UniqueDomainAccess with SingleIntIdModelAccess[Domain]

