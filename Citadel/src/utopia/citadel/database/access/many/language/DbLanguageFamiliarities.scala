package utopia.citadel.database.access.many.language

import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.citadel.database.model.user.UserLanguageModel
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.sql.{SelectAll, Where}

/**
  * Used for accessing multiple language familiarity levels at once
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1.0
  */
object DbLanguageFamiliarities extends ManyRowModelAccess[LanguageFamiliarity]
{
	// IMPLEMENTED	------------------------
	
	override def factory = LanguageFamiliarityFactory
	
	override def globalCondition = None
	
	override protected def defaultOrdering = None
	
	
	// OTHER	----------------------------
	
	/**
	  * @param userId     Id of the targeted user
	  * @param connection DB Connection (implicit)
	  * @return Language ids known to the targeted user, each paired with the user's familiarity level in that language
	  */
	def familiarityLevelsForUserWithId(userId: Int)(implicit connection: Connection) =
	{
		val linkModel = UserLanguageModel
		val target = factory.target.join(linkModel.table)
		val condition = linkModel.withUserId(userId)
		
		connection(SelectAll(target) + Where(condition)).rows.flatMap { row =>
			// Collects both language id and language familiarity
			factory.parseIfPresent(row).map { row(linkModel.table)(linkModel.languageIdAttName).getInt -> _ }
		}
	}
}
