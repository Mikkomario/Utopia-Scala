package utopia.citadel.database.access.single.user

import utopia.citadel.database.factory.user.UserLanguageLinkFactory
import utopia.citadel.database.model.user.UserLanguageLinkModel
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual UserLanguages
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserLanguageLink extends SingleRowModelAccess[UserLanguageLink] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserLanguageLinkModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageLinkFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserLanguage instance
	  * @return An access point to that UserLanguage
	  */
	def apply(id: Int) = DbSingleUserLanguageLink(id)
}

