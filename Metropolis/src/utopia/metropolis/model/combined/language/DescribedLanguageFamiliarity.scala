package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{Described, DescribedFromModelFactory}
import utopia.metropolis.model.stored.description.DescriptionLink
import utopia.metropolis.model.stored.language.LanguageFamiliarity

object DescribedLanguageFamiliarity extends DescribedFromModelFactory[DescribedLanguageFamiliarity, LanguageFamiliarity]
{
	override protected def undescribedFactory = LanguageFamiliarity
}

/**
  * Combines a language familiarity level with its descriptions
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
case class DescribedLanguageFamiliarity(wrapped: LanguageFamiliarity, descriptions: Set[DescriptionLink])
	extends Described[LanguageFamiliarity]
