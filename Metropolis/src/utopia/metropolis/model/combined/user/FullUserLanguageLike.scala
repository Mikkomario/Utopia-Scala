package utopia.metropolis.model.combined.user

import utopia.flow.collection.value.typeless.Constant
import utopia.flow.generic.ModelConvertible
import utopia.flow.generic.ValueConversions._
import utopia.metropolis.model.DeepExtender
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.user.UserLanguageLink

/**
  * A common trait for extended user language models
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
@deprecated("It may be better to use UserLanguageWithFamiliarity instead", "v2.0")
trait FullUserLanguageLike[+L <: ModelConvertible, +F <: ModelConvertible]
	extends DeepExtender[UserLanguageLink, UserLanguageLinkData] with ModelConvertible
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
	
	
	// IMPLEMENTED  ------------------------
	
	override def toModel = wrapped.toModel + Constant("language", language.toModel) +
		Constant("familiarity", familiarity.toModel)
	
	
	// OTHER	----------------------------
	
	/**
	  * @return A model representation of this user language link, without user id included
	  */
	def toModelWithoutUser = (wrapped.toModelWithoutUser - "language_id" - "familiarity_id") +
		Constant("language", language.toModel) + Constant("familiarity", familiarity.toModel)
}