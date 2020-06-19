package utopia.metropolis.model.combined.user

import utopia.metropolis.model.stored.language.Language
import utopia.metropolis.model.stored.user.UserLanguage

/**
  * A user language will full language data included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
case class FullUserLanguage(wrapped: UserLanguage, language: Language) extends FullUserLanguageLike[Language]