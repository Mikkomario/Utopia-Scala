package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, SimplyDescribed}
import utopia.metropolis.model.stored.description.{DescriptionLink, DescriptionRole}
import utopia.metropolis.model.stored.language.LanguageFamiliarity

object DescribedLanguageFamiliarity extends DescribedFromModelFactory[LanguageFamiliarity, DescribedLanguageFamiliarity]
{
	override protected def undescribedFactory = LanguageFamiliarity
}

/**
  * Combines a language familiarity level with its descriptions
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1
  */
case class DescribedLanguageFamiliarity(wrapped: LanguageFamiliarity, descriptions: Set[DescriptionLink])
	extends DescribedWrapper[LanguageFamiliarity] with SimplyDescribed
{
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}
