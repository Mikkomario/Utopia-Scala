package utopia.citadel.database.factory.user

import utopia.citadel.database.Tables
import utopia.flow.datastructure.template.{Model, Property}
import utopia.flow.generic.ValueUnwraps._
import utopia.metropolis.model.partial.user.UserLanguageLinkData
import utopia.metropolis.model.stored.user.UserLanguageLink
import utopia.vault.nosql.factory.row.model.FromRowModelFactory

/**
  * Used for reading user language links from the DB
  * @author Mikko Hilpinen
  * @since 17.5.2020, v1.0
  */
object UserLanguageFactory extends FromRowModelFactory[UserLanguageLink]
{
	override def apply(model: Model[Property]) = table.requirementDeclaration.validate(model).toTry.map { valid =>
		UserLanguageLink(valid("id"), UserLanguageLinkData(valid("userId"), valid("languageId"), valid("familiarityId")))
	}
	
	override def table = Tables.userLanguage
}
