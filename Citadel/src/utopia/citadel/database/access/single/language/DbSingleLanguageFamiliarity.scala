package utopia.citadel.database.access.single.language

import utopia.citadel.database.access.many.description.DbLanguageFamiliarityDescriptions
import utopia.citadel.database.access.single.description.{DbLanguageFamiliarityDescription, SingleIdDescribedAccess}
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity

/**
  * An access point to individual LanguageFamiliarities, based on their id
  * @since 2021-10-23
  */
case class DbSingleLanguageFamiliarity(id: Int) 
	extends UniqueLanguageFamiliarityAccess 
		with SingleIdDescribedAccess[LanguageFamiliarity, DescribedLanguageFamiliarity]
{
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedLanguageFamiliarity
	
	override protected def manyDescriptionsAccess = DbLanguageFamiliarityDescriptions
	
	override protected def singleDescriptionAccess = DbLanguageFamiliarityDescription
}

