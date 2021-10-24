package utopia.citadel.database.factory.user

import utopia.citadel.database.factory.language.{LanguageFactory, LanguageFamiliarityFactory}
import utopia.metropolis.model.combined.user.{FullUserLanguage, UserLanguageLinkWithFamiliarity}
import utopia.vault.model.immutable.Row
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.JoinType.Inner

/**
  * Used for reading user languages with language data included
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
object FullUserLanguageFactory extends FromRowFactory[FullUserLanguage]
{
	// COMPUTED ------------------------------
	
	/**
	  * @return Default ordering to use when reading data with this factory (based on familiarity order)
	  */
	def defaultOrder = LanguageFamiliarityFactory.defaultOrder
	
	
	// IMPLEMENTED	--------------------------
	
	override def joinedTables = LanguageFactory.tables ++ LanguageFamiliarityFactory.tables
	
	override def joinType = Inner
	
	override def table = UserLanguageLinkFactory.table
	
	// Parses language, familiarity and user language
	override def apply(row: Row) = UserLanguageLinkFactory(row).flatMap { link =>
		LanguageFactory(row).flatMap { language =>
			LanguageFamiliarityFactory(row).map { familiarity =>
				FullUserLanguage(UserLanguageLinkWithFamiliarity(link, familiarity), language)
			}
		}
	}
}
