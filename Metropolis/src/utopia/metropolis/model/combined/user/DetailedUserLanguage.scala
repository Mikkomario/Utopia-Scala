package utopia.metropolis.model.combined.user

import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
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
		userLink.toModel + Constant("language", language.toModel) + Constant("familiarity", familiarity.toModel)
	
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		language.toSimpleModelUsing(descriptionRoles) +
			Constant("familiarity", familiarity.toSimpleModelUsing(descriptionRoles))
}