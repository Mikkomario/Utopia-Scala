package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.citadel.database.access.many.description.DescriptionLinksAccessOld
import utopia.citadel.database.factory.description.DescriptionLinkFactoryOld
import utopia.citadel.database.model.description.DescriptionLinkModelFactoryOld

/**
  * Used for accessing scope descriptions
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbScopeDescriptions extends DescriptionLinksAccessOld
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * The model factory used by this access point
	  */
	override val linkModel = DescriptionLinkModelFactoryOld(AmbassadorTables.scopeDescription, "scopeId")
	/**
	  * The read factory used by this access point
	  */
	override val factory = DescriptionLinkFactoryOld(linkModel)
}
