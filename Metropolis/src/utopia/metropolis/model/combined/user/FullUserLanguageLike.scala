package utopia.metropolis.model.combined.user

import utopia.flow.datastructure.immutable.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.DeepExtender
import utopia.metropolis.model.partial.user.UserLanguageData
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.metropolis.model.stored.user.UserLanguage

/**
  * A common trait for extended user language models
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
trait FullUserLanguageLike[+L <: ModelConvertible, +F <: ModelConvertible]
	extends DeepExtender[UserLanguage, UserLanguageData]
{
	// ABSTRACT	----------------------------
	
	/**
	  * @return Linked language data
	  */
	def language: L
	
	/**
	  * @return This user's familiarity level in the specified language
	  */
	def familiarity: F
	
	
	// OTHER	----------------------------
	
	/**
	  * @return A model representation of this user language link, without user id included
	  */
	def toModelWithoutUser = (wrapped.toModelWithoutUser - "language_id" - "familiarity_id") +
		Constant("language", language.toModel) + Constant("familiarity", familiarity.toModel)
}