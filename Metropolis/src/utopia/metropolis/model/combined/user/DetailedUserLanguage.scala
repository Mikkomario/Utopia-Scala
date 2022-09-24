package utopia.metropolis.model.combined.user

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable
import utopia.flow.generic.model.immutable.Constant
import utopia.flow.generic.model.template.ModelConvertible
import utopia.flow.view.template.Extender
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.combined.language.{DescribedLanguage, DescribedLanguageFamiliarity}
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.user.UserLanguageLink

/**
  * A user language link with language data and language descriptions included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
case class DetailedUserLanguage(userLink: UserLanguageLink, language: DescribedLanguage,
                                familiarity: DescribedLanguageFamiliarity)
	extends Extender[UserLanguageLinkData] with DescribedSimpleModelConvertible with ModelConvertible
{
	def id = userLink.id
	
	override def wrapped = userLink.data
	
	override def toModel =
		userLink.toModel + immutable.Constant("language", language.toModel) + immutable.Constant("familiarity", familiarity.toModel)
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		language.toSimpleModelUsing(descriptionRoles) +
			Constant("familiarity", familiarity.toSimpleModelUsing(descriptionRoles))
}