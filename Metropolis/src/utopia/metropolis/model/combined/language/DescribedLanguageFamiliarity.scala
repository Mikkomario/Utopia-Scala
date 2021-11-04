package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{DescribedFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.language.LanguageFamiliarity

object DescribedLanguageFamiliarity 
	extends DescribedFactory[LanguageFamiliarity, DescribedLanguageFamiliarity]

/**
  * Combines LanguageFamiliarity with the linked descriptions
  * @param languageFamiliarity LanguageFamiliarity to wrap
  * @param descriptions Descriptions concerning the wrapped LanguageFamiliarity
  * @since 2021-10-23
  */
case class DescribedLanguageFamiliarity(languageFamiliarity: LanguageFamiliarity, 
	descriptions: Set[LinkedDescription])
	extends DescribedWrapper[LanguageFamiliarity] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = languageFamiliarity
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}

