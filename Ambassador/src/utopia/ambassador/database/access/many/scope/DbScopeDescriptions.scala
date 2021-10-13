package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.citadel.database.access.many.description.DescriptionLinksAccess
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.DescriptionLinkModelFactory

/**
  * Used for accessing scope descriptions
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbScopeDescriptions extends DescriptionLinksAccess
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * The model factory used by this access point
	  */
	override val linkModel = DescriptionLinkModelFactory(AmbassadorTables.scopeDescription, "scopeId")
	/**
	  * The read factory used by this access point
	  */
	override val linkFactory = DescriptionLinkFactory(linkModel)
}
