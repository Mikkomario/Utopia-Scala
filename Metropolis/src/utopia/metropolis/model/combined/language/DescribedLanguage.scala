package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{DescribedWrapper, DescribedFromModelFactory}
import utopia.metropolis.model.stored.description.DescriptionLink
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
case class DescribedLanguage(language: Language, descriptions: Set[DescriptionLink]) extends DescribedWrapper[Language]
{
	override def wrapped = language
}
