package utopia.exodus.database.access.single.auth

import utopia.exodus.model.stored.auth.ApiKey
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual ApiKeys, based on their id
  * @since 2021-10-25
  */
case class DbSingleApiKey(id: Int) extends UniqueApiKeyAccess with SingleIntIdModelAccess[ApiKey]

