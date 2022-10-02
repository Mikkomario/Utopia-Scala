package utopia.citadel.database.factory.user

import utopia.citadel.database.CitadelTables
import utopia.citadel.database.model.user.UserSettingsModel
import utopia.flow.generic.model.immutable.Model
import utopia.metropolis.model.partial.user.UserSettingsData
import utopia.metropolis.model.stored.user.UserSettings
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.nosql.template.Deprecatable

/**
  * Used for reading UserSettings data from the DB
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object UserSettingsFactory 
	extends FromValidatedRowModelFactory[UserSettings] with FromRowFactoryWithTimestamps[UserSettings] 
		with Deprecatable
{
	// IMPLEMENTED	--------------------
	
	override def creationTimePropertyName = "created"
	
	override def nonDeprecatedCondition = UserSettingsModel.nonDeprecatedCondition
	
	override def table = CitadelTables.userSettings
	
	override def fromValidatedModel(valid: Model) =
		UserSettings(valid("id").getInt, UserSettingsData(valid("userId").getInt, valid("name").getString, 
			valid("email").string, valid("created").getInstant, valid("deprecatedAfter").instant))
}