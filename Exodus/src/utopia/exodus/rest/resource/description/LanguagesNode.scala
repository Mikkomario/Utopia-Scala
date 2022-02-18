package utopia.exodus.rest.resource.description

import utopia.citadel.database.access.many.language.DbLanguages
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.nexus.rest.LeafResource
import utopia.vault.database.Connection

/**
  * Used for accessing all specified languages
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
object LanguagesNode extends GeneralDataNode[DescribedLanguage] with LeafResource[AuthorizedContext]
{
	// IMPLEMENTED	--------------------------------
	
	override val name = "languages"
	
	override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
		DbLanguages.described
}
