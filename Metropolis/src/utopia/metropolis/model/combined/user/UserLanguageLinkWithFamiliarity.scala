package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ValueConversions._
import utopia.flow.util.Extender
import utopia.metropolis.model.StyledModelConvertible
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.metropolis.model.stored.user.UserLanguageLink

/**
  * Combines languageLink with familiarity data
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
case class UserLanguageLinkWithFamiliarity(languageLink: UserLanguageLink, familiarity: LanguageFamiliarity)
	extends Extender[UserLanguageLinkData] with StyledModelConvertible
{
	// COMPUTED	--------------------
	
	/**
	  * Id of this languageLink in the database
	  */
	def id = languageLink.id
	
	
	// IMPLEMENTED	--------------------
	
	override def wrapped = languageLink.data
	
	override def toModel = languageLink.toModel + Constant("familiarity", familiarity.toModel)
	
	override def toSimpleModel =
		languageLink.toSimpleModel - "familiarity_id" + Constant("familiarity", familiarity.toSimpleModel)
}

