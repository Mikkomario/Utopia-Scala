package utopia.citadel.database.access.many.language

import utopia.citadel.database.access.many.description.{DbLanguageFamiliarityDescriptions, ManyDescribedAccess}
import utopia.citadel.database.factory.language.LanguageFamiliarityFactory
import utopia.citadel.database.model.user.UserLanguageLinkModel
import utopia.metropolis.model.combined.language.DescribedLanguageFamiliarity
import utopia.metropolis.model.stored.language.LanguageFamiliarity
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.UnconditionalView
import utopia.vault.sql.{SelectAll, Where}

/**
  * Used for accessing multiple language familiarity levels at once
  * @author Mikko Hilpinen
  * @since 25.7.2020, v1.0
  */
// TODO: Add default ordering based on order index
object DbLanguageFamiliarities
	extends ManyRowModelAccess[LanguageFamiliarity]
		with ManyDescribedAccess[LanguageFamiliarity, DescribedLanguageFamiliarity] with UnconditionalView
{
	// IMPLEMENTED	------------------------
	
	override def factory = LanguageFamiliarityFactory
	
	override protected def defaultOrdering = None
	
	override protected def manyDescriptionsAccess =
		DbLanguageFamiliarityDescriptions
	override protected def describedFactory =
		DescribedLanguageFamiliarity
	
	override protected def idOf(item: LanguageFamiliarity) = item.id
	
	
	// OTHER	----------------------------
	
	/**
	  * @param userId     Id of the targeted user
	  * @param connection DB Connection (implicit)
	  * @return Language ids known to the targeted user, each paired with the user's familiarity level in that language
	  */
	def familiarityLevelsForUserWithId(userId: Int)(implicit connection: Connection) =
	{
		val linkModel = UserLanguageLinkModel
		val target = factory.target.join(linkModel.table)
		val condition = linkModel.withUserId(userId)
		
		connection(SelectAll(target) + Where(condition)).rows.flatMap { row =>
			// Collects both language id and language familiarity
			factory.parseIfPresent(row).map { row(linkModel.table)(linkModel.languageIdAttName).getInt -> _ }
		}
	}
}
