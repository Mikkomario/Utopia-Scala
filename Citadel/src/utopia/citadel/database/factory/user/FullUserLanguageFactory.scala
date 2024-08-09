package utopia.citadel.database.factory.user

import utopia.citadel.database.factory.language.{LanguageFactory, LanguageFamiliarityFactory}
import utopia.metropolis.model.combined.user.{FullUserLanguage, UserLanguageLinkWithFamiliarity}
import utopia.vault.model.enumeration.SelectTarget
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
	// ATTRIBUTES   --------------------------
	
	override lazy val joinedTables = languageFactory.tables ++ familiarityFactory.tables
	
	override lazy val selectTarget: SelectTarget =
		Vector(languageLinkFactory, languageFactory, familiarityFactory).view.map { _.selectTarget }.reduce { _ + _ }
	
	
	// COMPUTED ------------------------------
	
	private def languageLinkFactory = UserLanguageLinkFactory
	private def languageFactory = LanguageFactory
	private def familiarityFactory = LanguageFamiliarityFactory
	
	/**
	  * @return Default ordering to use when reading data with this factory (based on familiarity order)
	  */
	def defaultOrder = LanguageFamiliarityFactory.defaultOrder
	
	
	// IMPLEMENTED	--------------------------
	
	override def table = languageLinkFactory.table
	override def joinType = Inner
	
	override def defaultOrdering = Some(defaultOrder)
	
	// Parses language, familiarity and user language
	override def apply(row: Row) = languageLinkFactory(row).flatMap { link =>
		languageFactory(row).flatMap { language =>
			familiarityFactory(row).map { familiarity =>
				FullUserLanguage(UserLanguageLinkWithFamiliarity(link, familiarity), language)
			}
		}
	}
}
