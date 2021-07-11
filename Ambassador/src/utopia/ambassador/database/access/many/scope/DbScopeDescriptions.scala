package utopia.ambassador.database.access.many.scope

import utopia.ambassador.database.AmbassadorTables
import utopia.citadel.database.access.many.description.DbDescriptions.{DescriptionsOfAll, DescriptionsOfMany, DescriptionsOfSingle}
import utopia.citadel.database.factory.description.DescriptionLinkFactory
import utopia.citadel.database.model.description.DescriptionLinkModelFactory

/**
  * Used for accessing scope descriptions
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.0
  */
object DbScopeDescriptions
{
	// ATTRIBUTES   ----------------------------
	
	/**
	  * The model factory used by this access point
	  */
	val model = DescriptionLinkModelFactory(AmbassadorTables.scopeDescription, "scopeId")
	/**
	  * The read factory used by this access point
	  */
	val factory = DescriptionLinkFactory(model)
	
	/**
	  * An access point to all scope descriptions
	  */
	val all = DescriptionsOfAll(factory, model)
	
	
	// OTHER    ---------------------------------
	
	/**
	  * @param scopeId Id of a scope
	  * @return An access point to that scope's descriptions
	  */
	def apply(scopeId: Int) = DescriptionsOfSingle(scopeId, factory, model)
	/**
	  * @param scopeIds A set of scope ids
	  * @return An access point to descriptions of those scopes
	  */
	def apply(scopeIds: Set[Int]) = DescriptionsOfMany(scopeIds, factory, model)
}
