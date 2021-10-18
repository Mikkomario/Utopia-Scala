package utopia.citadel.util

import utopia.citadel.database.access.single.language.DbLanguage
import utopia.citadel.database.access.single.user.DbUser
import utopia.metropolis.model.stored.language.Language
import utopia.metropolis.model.stored.user.User

/**
 * Provides implicit extensions for easily accessing stored items in the database from the Metropolis project
 * @author Mikko Hilpinen
 * @since 12.10.2021, v1.3
 */
object MetropolisAccessExtensions
{
	implicit class AccessibleUser(val u: User) extends AnyVal
	{
		/**
		 * @return An access point to this user's data in the database
		 */
		def access = DbUser(u.id)
	}
	
	implicit class AccessibleLanguage(val l: Language) extends AnyVal
	{
		/**
		 * @return An access point to this language's data in the database
		 */
		def access = DbLanguage(l.id)
	}
}
