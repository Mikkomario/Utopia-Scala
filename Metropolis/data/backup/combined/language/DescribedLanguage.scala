package utopia.metropolis.model.combined.language

import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, SimplyDescribed}
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.stored.description.{DescriptionLinkOld, DescriptionRole}
import utopia.metropolis.model.stored.language.Language

object DescribedLanguage extends DescribedFromModelFactory[Language, DescribedLanguage]
{
	override protected def undescribedFactory = Language
}

/**
  * Adds descriptive data to a language
  * @author Mikko Hilpinen
  * @since 16.5.2020, v1
  */
case class DescribedLanguage(language: Language, descriptions: Set[DescriptionLinkOld])
	extends DescribedWrapper[Language] with SimplyDescribed
{
	// IMPLEMENTED  ---------------------------------
	
	override def wrapped = language
	
	override protected def simpleBaseModel(roles: Iterable[DescriptionRole]) = wrapped.toModel
	
	
	// OTHER    -------------------------------------
	
	/**
	 * @param descriptionRole Targeted description role
	 * @return Value for that role or the ISO-code of this language
	 */
	def descriptionOrCode(descriptionRole: DescriptionRoleIdWrapper) =
		apply(descriptionRole).getOrElse(language.isoCode)
}
