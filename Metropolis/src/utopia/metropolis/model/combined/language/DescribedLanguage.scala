package utopia.metropolis.model.combined.language

import utopia.flow.util.StringExtensions._
import utopia.metropolis.model.combined.description.{DescribedFromModelFactory, DescribedWrapper, LinkedDescription, SimplyDescribed}
import utopia.metropolis.model.enumeration.DescriptionRoleIdWrapper
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.language.Language

object DescribedLanguage extends DescribedFromModelFactory[Language, DescribedLanguage]
{
	override protected def undescribedFactory = Language
}

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
	
	
	// OTHER    ------------------------
	
	/**
	 * @param role A description role
	 * @return That description text of this language, if available - otherwise language code
	 */
	def descriptionOrCode(role: DescriptionRoleIdWrapper) = apply(role).nonEmptyOrElse(language.isoCode)
}

