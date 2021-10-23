package utopia.citadel.database.access.single.language

import utopia.citadel.database.access.many.description.DbLanguageFamiliarityDescriptions
import utopia.citadel.database.access.single.description.{DbLanguageFamiliarityDescription, SingleIdDescribedAccess}
import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual familiarity levels
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1.0
  */
object DbLanguageFamiliarity extends SingleRowModelAccess[LanguageFamiliarity] with UnconditionalView
{
	override def factory = LanguageFamiliarityFactory
	
	def apply(familiarityId: Int) = DbSingleLanguageFamiliarity(familiarityId)
	
	case class DbSingleLanguageFamiliarity(id: Int)
		extends SingleIdDescribedAccess[LanguageFamiliarity, DescribedLanguageFamiliarity]
	{
		override protected def singleDescriptionAccess =
			DbLanguageFamiliarityDescription
		override protected def manyDescriptionsAccess =
			DbLanguageFamiliarityDescriptions
		override protected def describedFactory = DescribedLanguageFamiliarity
		
		override def factory = DbLanguageFamiliarity.factory
	}
}
