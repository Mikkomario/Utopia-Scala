package utopia.exodus.database.access.single

import utopia.exodus.database.factory.language.LanguageFamiliarityFactory
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.access.SingleModelAccessById

/**
  * Used for accessing individual familiarity levels
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
@deprecated("Please use the Citadel version instead", "v2.0")
object DbLanguageFamiliarity extends SingleModelAccessById[LanguageFamiliarity, Int]
{
	override def factory = LanguageFamiliarityFactory
	
	override def idToValue(id: Int) = id
}
