package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{DescribedFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.language.Language

object DescribedLanguage extends DescribedFactory[Language, DescribedLanguage]

/**
  * Combines Language with the linked descriptions
  * @param language Language to wrap
  * @param descriptions Descriptions concerning the wrapped Language
  * @since 2021-10-23
  */
case class DescribedLanguage(language: Language, descriptions: Set[LinkedDescription])
	extends DescribedWrapper[Language] with SimplyDescribed
{
	// IMPLEMENTED	--------------------
	
	override def wrapped = language
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}

