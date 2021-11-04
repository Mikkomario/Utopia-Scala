package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionRoleFactory
import utopia.citadel.database.model.description.DescriptionRoleModel
import utopia.metropolis.model.stored.description.DescriptionRole
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.UnconditionalView

/**
  * Used for accessing individual DescriptionRoles
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbDescriptionRole extends SingleRowModelAccess[DescriptionRole] with UnconditionalView with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DescriptionRoleModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionRoleFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted DescriptionRole instance
	  * @return An access point to that DescriptionRole
	  */
	def apply(id: Int) = DbSingleDescriptionRole(id)
}

