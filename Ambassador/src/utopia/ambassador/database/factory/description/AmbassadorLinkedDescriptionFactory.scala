package utopia.ambassador.database.factory.description

import utopia.citadel.database.factory.description.LinkedDescriptionFactory

object AmbassadorLinkedDescriptionFactory
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Factory for reading descriptions linked with Scopes
	  */
	lazy val scope = LinkedDescriptionFactory(AmbassadorDescriptionLinkFactory.scope)
}

