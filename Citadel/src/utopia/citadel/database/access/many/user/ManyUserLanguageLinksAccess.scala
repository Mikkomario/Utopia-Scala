package utopia.citadel.database.access.many.user

import utopia.citadel.database.access.many.language.{DbLanguageFamiliarities, DbLanguages}
import utopia.citadel.database.factory.user.UserLanguageLinkFactory
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.user.DetailedUserLanguage
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.database.Connection
import utopia.vault.nosql.access.many.model.ManyRowModelAccess
import utopia.vault.nosql.view.SubView
import utopia.vault.sql.Condition

object ManyUserLanguageLinksAccess
{
	// NESTED	--------------------
	
	private class ManyUserLanguageLinksSubView(override val parent: ManyRowModelAccess[UserLanguageLink],
	                                           override val filterCondition: Condition)
		extends ManyUserLanguageLinksAccess with SubView
}

/**
  * A common trait for access points which target multiple UserLanguages at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
trait ManyUserLanguageLinksAccess
	extends ManyUserLanguageLinksAccessLike[UserLanguageLink, ManyUserLanguageLinksAccess]
{
	// COMPUTED ------------------------
	
	/**
	  * @return A version of this access point which includes language familiarity data
	  */
	def withFamiliarities = globalCondition match
	{
		case Some(condition) => DbUserLanguageLinksWithFamiliarities.filter(condition)
		case None => DbUserLanguageLinksWithFamiliarities
	}
	/**
	  * @return A version of this access point which includes language and language familiarity data
	  */
	def full = globalCondition match
	{
		case Some(condition) => DbFullUserLanguages.filter(condition)
		case None => DbFullUserLanguages
	}
	/**
	  * @param connection Implicit DB Connection
	  * @param languageIds Ids of the languages in which descriptions are read
	  * @return Detailed copies of available language links (include described language and familiarity data)
	  */
	def detailed(implicit connection: Connection, languageIds: LanguageIds) =
	{
		// Reads the links and the described language & familiarity data
		val links = pull
		val languages = DbLanguages(links.map { _.languageId }.toSet).described
		val familiarities = DbLanguageFamiliarities(links.map { _.familiarityId }.toSet).described
		
		val languageById = languages.map { l => l.id -> l }.toMap
		val familiarityById = familiarities.map { f => f.id -> f }.toMap
		
		// Combines the data into detailed links
		links.map { link => DetailedUserLanguage(link, languageById(link.languageId),
			familiarityById(link.familiarityId)) }
	}
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = UserLanguageLinkFactory
	
	override def _filter(additionalCondition: Condition): ManyUserLanguageLinksAccess =
		new ManyUserLanguageLinksAccess.ManyUserLanguageLinksSubView(this, additionalCondition)
}

