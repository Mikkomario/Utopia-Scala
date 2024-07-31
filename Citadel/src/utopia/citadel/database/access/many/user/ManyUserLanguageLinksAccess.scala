package utopia.citadel.database.access.many.user

import utopia.citadel.database.access.many.language.{DbLanguageFamiliarities, DbLanguages}
import utopia.citadel.database.factory.user.UserLanguageLinkFactory
import utopia.metropolis.model.cached.LanguageIds
import utopia.metropolis.model.combined.user.DetailedUserLanguage
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.database.Connection
import utopia.vault.sql.Condition

object ManyUserLanguageLinksAccess
{
	// OTHER	--------------------
	
	def apply(condition: Condition): ManyUserLanguageLinksAccess = _Access(Some(condition))
	
	
	// NESTED	--------------------
	
	private case class _Access(accessCondition: Option[Condition]) extends ManyUserLanguageLinksAccess
}

/**
  * A common trait for access points which target multiple UserLanguages at a time
  * @author Mikko Hilpinen
  * @since 23.10.2021
  */
trait ManyUserLanguageLinksAccess 
	extends ManyUserLanguageLinksAccessLike[UserLanguageLink, ManyUserLanguageLinksAccess]
{
	// COMPUTED	--------------------
	
	/**
	  * A version of this access point which includes language familiarity data
	  */
	def withFamiliarities = {
		accessCondition match
		{
			case Some(condition) => DbUserLanguageLinksWithFamiliarities.filter(condition)
			case None => DbUserLanguageLinksWithFamiliarities
		}
	}
	
	/**
	  * A version of this access point which includes language and language familiarity data
	  */
	def full = {
		accessCondition match
		{
			case Some(condition) => DbFullUserLanguages.filter(condition)
			case None => DbFullUserLanguages
		}
	}
	
	/**
	  * Detailed copies of available language links (include described language and familiarity data)
	  * @param connection Implicit DB Connection
	  * @param languageIds Ids of the languages in which descriptions are read
	  */
	def detailed(implicit connection: Connection, languageIds: LanguageIds) = {
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
	
	override protected def self = this
	
	override
		 def apply(condition: Condition): ManyUserLanguageLinksAccess = ManyUserLanguageLinksAccess(condition)
}

