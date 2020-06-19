package utopia.exodus.rest.resource.description

import utopia.exodus.database.access.many.{DbDescriptions, DbLanguages}
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.metropolis.model.stored.language.Language
import utopia.vault.database.Connection

/**
  * Used for accessing all specified languages
  * @author Mikko Hilpinen
  * @since 20.5.2020, v1
  */
object LanguagesNode extends PublicDescriptionsNode[Language, DescribedLanguage]
{
	// IMPLEMENTED	--------------------------------
	
	override val name = "languages"
	
	override protected def items(implicit connection: Connection) = DbLanguages.all
	
	override protected def descriptionsAccess = DbDescriptions.ofAllLanguages
	
	override protected def idOf(item: Language) = item.id
	
	override protected def combine(item: Language, descriptions: Set[DescriptionLink]) =
		DescribedLanguage(item, descriptions)
}
