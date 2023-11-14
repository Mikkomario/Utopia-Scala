package utopia.citadel.database.factory.description

import utopia.citadel.database.CitadelTables

object CitadelDescriptionLinkFactory
{
	// ATTRIBUTES	--------------------
	
	lazy val descriptionRole = DescriptionLinkFactory(CitadelTables.descriptionRoleDescription)
	lazy val language = DescriptionLinkFactory(CitadelTables.languageDescription)
	lazy val languageFamiliarity = DescriptionLinkFactory(CitadelTables.languageFamiliarityDescription)
	lazy val organization = DescriptionLinkFactory(CitadelTables.organizationDescription)
	lazy val task = DescriptionLinkFactory(CitadelTables.taskDescription)
	lazy val userRole = DescriptionLinkFactory(CitadelTables.userRoleDescription)
}
