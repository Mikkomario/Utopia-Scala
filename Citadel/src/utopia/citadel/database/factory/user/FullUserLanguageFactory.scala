package utopia.citadel.database.factory.user

import utopia.citadel.database.factory.language.{LanguageFactory, LanguageFamiliarityFactory}
import utopia.metropolis.model.combined.user.FullUserLanguage
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.FromRowFactory
import utopia.vault.sql.JoinType.Inner

/**
  * Used for reading user languages with language data included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
object FullUserLanguageFactory extends FromRowFactory[FullUserLanguage]
{
	// IMPLEMENTED	--------------------------
	
	// Parses language, familiarity and user language
	override def apply(row: Row) = UserLanguageFactory(row).flatMap { link =>
		LanguageFactory(row).flatMap { language =>
			LanguageFamiliarityFactory(row).map { familiarity =>
				FullUserLanguage(link, language, familiarity)
			}
		}
	}
	
	override def joinedTables = LanguageFactory.tables ++ LanguageFamiliarityFactory.tables
	
	override def joinType = Inner
	
	override def table = UserLanguageFactory.table
}
