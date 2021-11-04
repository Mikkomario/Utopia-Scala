package utopia.citadel.database.access.single.language

import utopia.citadel.database.access.many.description.DbLanguageDescriptions
import utopia.citadel.database.access.single.description.{DbLanguageDescription, SingleIdDescribedAccess}
import utopia.metropolis.model.combined.language.DescribedLanguage
import utopia.metropolis.model.stored.language.Language

/**
  * An access point to individual Languages, based on their id
  * @since 2021-10-23
  */
case class DbSingleLanguage(id: Int) 
	extends UniqueLanguageAccess with SingleIdDescribedAccess[Language, DescribedLanguage]
{
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedLanguage
	
	override protected def manyDescriptionsAccess = DbLanguageDescriptions
	
	override protected def singleDescriptionAccess = DbLanguageDescription
}

