package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, SimplyDescribed}
import utopia.metropolis.model.stored.description.{DescriptionLink, DescriptionRole}
import utopia.metropolis.model.stored.language.Language

object DescribedLanguage extends DescribedFromModelFactory[DescribedLanguage, Language]
{
	override protected def undescribedFactory = Language
}

/**
  * Adds descriptive data to a language
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
case class DescribedLanguage(language: Language, descriptions: Set[DescriptionLink])
	extends DescribedWrapper[Language] with SimplyDescribed
{
	override def wrapped = language
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
}
