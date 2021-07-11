package utopia.citadel.database.factory.user

import utopia.citadel.database.Tables
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

object UserSettingsFactory extends FromValidatedRowModelFactory[UserSettings] with Deprecatable
{
	// IMPLEMENTED	----------------------------------
	
	override def table = Tables.userSettings
	
	override protected def fromValidatedModel(model: Model[Constant]) = UserSettings(model("id").getInt,
		model(UserSettingsModel.userIdAttName).getInt, UserSettingsData(model("name").getString,
			model("email").getString, model("created").getInstant))
	
	override val nonDeprecatedCondition = table("deprecatedAfter").isNull
}


