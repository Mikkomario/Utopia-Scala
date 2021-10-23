package utopia.citadel.database.access.single.user

import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual UserLanguages, based on their id
  * @since 2021-10-23
  */
case class DbSingleUserLanguage(id: Int) 
	extends UniqueUserLanguageAccess with SingleIntIdModelAccess[UserLanguage]

