package utopia.citadel.database.access.single.user

import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual UserLanguages, based on their id
  * @since 2021-10-23
  */
case class DbSingleUserLanguageLink(id: Int)
	extends UniqueUserLanguageLinkAccess with SingleIntIdModelAccess[UserLanguageLink]

