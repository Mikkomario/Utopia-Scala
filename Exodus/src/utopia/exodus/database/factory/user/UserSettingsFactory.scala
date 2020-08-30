package utopia.exodus.database.factory.user

import utopia.exodus.database.Tables
import utopia.exodus.database.model.user.UserSettingsModel
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.nosql.factory.{Deprecatable, FromValidatedRowModelFactory}

object UserSettingsFactory extends FromValidatedRowModelFactory[UserSettings] with Deprecatable
{
	// IMPLEMENTED	----------------------------------
	
	override def table = Tables.userSettings
	
	override protected def fromValidatedModel(model: Model[Constant]) = UserSettings(model("id").getInt,
		model(UserSettingsModel.userIdAttName).getInt, UserSettingsData(model("name").getString, model("email").getString))
	
	override val nonDeprecatedCondition = table("deprecatedAfter").isNull
}


