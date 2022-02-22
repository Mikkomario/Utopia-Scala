package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.Scope
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual scopes, based on their id
  * @author Mikko Hilpinen
  * @since 18.02.2022, v4.0
  */
case class DbSingleScope(id: Int) extends UniqueScopeAccess with SingleIntIdModelAccess[Scope]

