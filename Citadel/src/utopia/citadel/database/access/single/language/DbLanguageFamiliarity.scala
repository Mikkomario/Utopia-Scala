package utopia.citadel.database.access.single.language

import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.access.single.model.SingleModelAccessById

/**
  * Used for accessing individual familiarity levels
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1.0
  */
object DbLanguageFamiliarity extends SingleModelAccessById[LanguageFamiliarity, Int]
{
	override def factory = LanguageFamiliarityFactory
	
	override def idToValue(id: Int) = id
}
