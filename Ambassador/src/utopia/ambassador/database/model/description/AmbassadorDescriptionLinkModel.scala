package utopia.ambassador.database.model.description

import utopia.ambassador.database.AmbassadorTables
import utopia.citadel.database.model.description.DescriptionLinkModelFactory

object AmbassadorDescriptionLinkModel
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Database interaction model factory for Scope description links
	  */
	lazy val scope = DescriptionLinkModelFactory(AmbassadorTables.scopeDescription)
}

