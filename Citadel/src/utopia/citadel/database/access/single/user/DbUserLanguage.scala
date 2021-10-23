package utopia.citadel.database.access.single.user

import utopia.citadel.database.factory.user.UserLanguageFactory
import utopia.citadel.database.model.user.UserLanguageModel
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual UserLanguages
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbUserLanguage extends SingleRowModelAccess[UserLanguage] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = UserLanguageModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted UserLanguage instance
	  * @return An access point to that UserLanguage
	  */
	def apply(id: Int) = DbSingleUserLanguage(id)
}

