package utopia.ambassador.database.access.single.scope

import utopia.ambassador.database.access.many.description.DbScopeDescriptions
import utopia.ambassador.database.access.single.description.DbScopeDescription
import utopia.ambassador.model.combined.scope.DescribedScope
import utopia.ambassador.model.stored.scope.Scope
import utopia.citadel.database.access.single.description.SingleIdDescribedAccess

/**
  * An access point to individual Scopes, based on their id
  * @author Mikko Hilpinen
  * @since 2021-10-26
  */
case class DbSingleScope(id: Int) 
	extends UniqueScopeAccess with SingleIdDescribedAccess[Scope, DescribedScope]
{
	// IMPLEMENTED	--------------------
	
	override protected def describedFactory = DescribedScope
	
	override protected def manyDescriptionsAccess = DbScopeDescriptions
	
	override protected def singleDescriptionAccess = DbScopeDescription
}

