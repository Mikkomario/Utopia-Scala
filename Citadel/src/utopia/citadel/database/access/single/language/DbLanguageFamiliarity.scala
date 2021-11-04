package utopia.citadel.database.access.single.language

import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.citadel.database.model.language.LanguageFamiliarityModel
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual LanguageFamiliarities
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbLanguageFamiliarity 
	extends SingleRowModelAccess[LanguageFamiliarity] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = LanguageFamiliarityModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = LanguageFamiliarityFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted LanguageFamiliarity instance
	  * @return An access point to that LanguageFamiliarity
	  */
	def apply(id: Int) = DbSingleLanguageFamiliarity(id)
}

