package utopia.citadel.database.access.many.description

import utopia.metropolis.model.combined.description.DescribedDescriptionRole
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.nosql.view.UnconditionalView

/**
  * The root access point when targeting multiple DescriptionRoles at a time
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbDescriptionRoles extends ManyDescriptionRolesAccess with UnconditionalView
{
	// OTHER	--------------------
	
	/**
	  * @param ids Ids of the targeted DescriptionRoles
	  * @return An access point to DescriptionRoles with the specified ids
	  */
	def apply(ids: Set[Int]) = new DbDescriptionRolesSubset(ids)
	
	
	// NESTED	--------------------
	
	class DbDescriptionRolesSubset(override val ids: Set[Int]) 
		extends ManyDescriptionRolesAccess 
			with ManyDescribedAccessByIds[DescriptionRole, DescribedDescriptionRole]
}

