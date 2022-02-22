package utopia.exodus.rest.resource.description

import utopia.citadel.database.access.many.language.DbLanguageFamiliarities
import utopia.exodus.rest.util.AuthorizedContext
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.nexus.rest.LeafResource
import utopia.vault.database.Connection

/**
  * Used for accessing language familiarity levels and their descriptions
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
object LanguageFamiliaritiesNode
	extends GeneralDataNode[DescribedLanguageFamiliarity] with LeafResource[AuthorizedContext]
{
	override def name = "language-familiarities"
	
	override protected def describedItems(implicit connection: Connection, languageIds: LanguageIds) =
		DbLanguageFamiliarities.described
}
