package utopia.ambassador.database.factory.description

import utopia.ambassador.database.AmbassadorTables
import utopia.citadel.database.factory.description.DescriptionLinkFactory

object AmbassadorDescriptionLinkFactory
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Factory for reading Scope description links
	  */
	lazy val scope = DescriptionLinkFactory(AmbassadorTables.scopeDescription)
}

