package utopia.citadel.database.access.many.user

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for reading user language links with linked data included
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
object DbFullUserLanguages extends ManyFullUserLanguagesAccess with UnconditionalView
