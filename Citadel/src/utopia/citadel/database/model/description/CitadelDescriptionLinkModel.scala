package utopia.citadel.database.model.description

import utopia.citadel.database.CitadelTables

/**
  * Contains references to different description link model factories from this (Citadel) project
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
object CitadelDescriptionLinkModel
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Database interaction model factory for ClientDevice description links
	  */
	lazy val clientDevice = DescriptionLinkModelFactory(CitadelTables.clientDeviceDescription)
	/**
	  * Database interaction model factory for DescriptionRole description links
	  */
	lazy val descriptionRole = DescriptionLinkModelFactory(CitadelTables.descriptionRoleDescription)
	/**
	  * Database interaction model factory for Language description links
	  */
	lazy val language = DescriptionLinkModelFactory(CitadelTables.languageDescription)
	/**
	  * Database interaction model factory for LanguageFamiliarity description links
	  */
	lazy val languageFamiliarity =
		DescriptionLinkModelFactory(CitadelTables.languageFamiliarityDescription)
	/**
	  * Database interaction model factory for Organization description links
	  */
	lazy val organization = DescriptionLinkModelFactory(CitadelTables.organizationDescription)
	/**
	  * Database interaction model factory for Task description links
	  */
	lazy val task = DescriptionLinkModelFactory(CitadelTables.taskDescription)
	/**
	  * Database interaction model factory for UserRole description links
	  */
	lazy val userRole = DescriptionLinkModelFactory(CitadelTables.userRoleDescription)
}
