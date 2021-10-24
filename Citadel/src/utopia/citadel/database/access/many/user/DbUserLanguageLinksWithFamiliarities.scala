package utopia.citadel.database.access.many.user

import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing multiple user language links at a time while including language familiarity information
  * @author Mikko Hilpinen
  * @since 24.10.2021, v2.0
  */
object DbUserLanguageLinksWithFamiliarities extends ManyUserLanguageLinksWithFamiliaritiesAccess with UnconditionalView
