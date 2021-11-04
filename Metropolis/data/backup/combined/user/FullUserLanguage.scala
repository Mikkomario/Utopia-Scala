package utopia.metropolis.model.combined.user

import utopia.metropolis.model.stored.language.{Language, LanguageFamiliarity}
import utopia.metropolis.model.stored.user.UserLanguageLink

/**
  * A user language will full language and familiarity data included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
case class FullUserLanguage(wrapped: UserLanguageLink, language: Language, familiarity: LanguageFamiliarity)
	extends FullUserLanguageLike[Language, LanguageFamiliarity]