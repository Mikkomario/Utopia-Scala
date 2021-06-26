package utopia.citadel.database.factory.user

import utopia.citadel.database.Tables
import utopia.citadel.database.access.single.DbUser
import utopia.flow.datastructure.immutable.{Constant, Model}
import utopia.metropolis.model.combined.user.UserWithLinks
import utopia.metropolis.model.stored.user.{User, UserSettings}
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.{Deprecatable, LinkedFactory}

object UserFactory extends LinkedFactory[User, UserSettings] with Deprecatable
{
	// IMPLEMENTED	-----------------------------------
	
	override def table = Tables.user
	
	override def childFactory = UserSettingsFactory
	
	override def apply(model: Model[Constant], child: UserSettings) =
		table.requirementDeclaration.validate(model).toTry.map { valid => User(valid("id").getInt, child) }
	
	override def nonDeprecatedCondition = UserSettingsFactory.nonDeprecatedCondition
	
	
	// OTHER	-------------------------------------
	
	/**
	  * Completes a normal user's data to include linked language and device ids
	  * @param user User to complete
	  * @param connection DB Connection
	  * @return User with associated data added
	  */
	def complete(user: User)(implicit connection: Connection) =
	{
		// Reads language links
		val languages = DbUser(user.id).languages.all
		// Reads device links
		val deviceIds = DbUser(user.id).deviceIds
		
		// Combines data
		UserWithLinks(user, languages, deviceIds)
	}
}


