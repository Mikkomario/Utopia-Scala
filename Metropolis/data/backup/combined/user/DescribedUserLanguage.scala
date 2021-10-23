package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.combined.description.DescribedSimpleModelConvertible
import utopia.metropolis.model.combined.language.{DescribedLanguage, DescribedLanguageFamiliarity}
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.metropolis.model.stored.user.UserLanguage

/**
  * A user language link with language data and language descriptions included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
case class DescribedUserLanguage(wrapped: UserLanguage, language: DescribedLanguage,
								 familiarity: DescribedLanguageFamiliarity)
	extends FullUserLanguageLike[DescribedLanguage, DescribedLanguageFamiliarity] with DescribedSimpleModelConvertible
{
	override def toSimpleModelUsing(descriptionRoles: Iterable[DescriptionRole]) =
		language.toSimpleModelUsing(descriptionRoles) +
			Constant("familiarity", familiarity.toSimpleModelUsing(descriptionRoles))
}