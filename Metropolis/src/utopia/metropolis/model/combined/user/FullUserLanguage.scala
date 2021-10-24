package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.{DeepExtender, StyledModelConvertible}
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.language.Language

/**
  * A user language will full language and familiarity data included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
case class FullUserLanguage(wrapped: UserLanguageLinkWithFamiliarity, language: Language)
	extends DeepExtender[UserLanguageLinkWithFamiliarity, UserLanguageLinkData] with StyledModelConvertible
{
	override def toSimpleModel = wrapped.toSimpleModel + Constant("language", language.toSimpleModel)
	
	override def toModel = wrapped.toModel + Constant("language", language.toModel)
}