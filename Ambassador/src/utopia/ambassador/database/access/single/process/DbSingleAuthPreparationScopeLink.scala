package utopia.ambassador.database.access.single.process

import utopia.ambassador.model.stored.process.AuthPreparationScopeLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual AuthPreparationScopeLinks, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleAuthPreparationScopeLink(id: Int) 
	extends UniqueAuthPreparationScopeLinkAccess with SingleIntIdModelAccess[AuthPreparationScopeLink]

