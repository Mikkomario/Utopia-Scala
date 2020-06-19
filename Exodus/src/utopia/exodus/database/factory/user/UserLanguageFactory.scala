package utopia.exodus.database.factory.user

import utopia.exodus.database.Tables
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.enumeration.LanguageFamiliarity
import utopia.metropolis.model.partial.user.UserLanguageData
import utopia.metropolis.model.stored.user.UserLanguage
import utopia.vault.nosql.factory.FromRowModelFactory

/**
  * Used for reading user language links from the DB
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1
  */
object UserLanguageFactory extends FromRowModelFactory[UserLanguage]
{
	// Familiarity must be parseable
	override def apply(model: Model[Property]) = table.requirementDeclaration.validate(model).toTry.flatMap { valid =>
		LanguageFamiliarity.forId(valid("familiarityId")).map { familiarity =>
			UserLanguage(valid("id"), UserLanguageData(valid("userId"), valid("languageId"), familiarity))
		}
	}
	
	override def table = Tables.userLanguage
}
