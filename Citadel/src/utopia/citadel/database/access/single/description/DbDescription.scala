package utopia.citadel.database.access.single.description

import utopia.citadel.database.factory.description.DescriptionFactory
import utopia.citadel.database.model.description.DescriptionModel
import utopia.metropolis.model.stored.description.Description
import utopia.vault.nosql.access.single.model.SingleRowModelAccess
import utopia.vault.nosql.template.Indexed
import utopia.vault.nosql.view.NonDeprecatedView

/**
  * Used for accessing individual Descriptions
  * @author Mikko Hilpinen
  * @since 2021-10-23
  */
object DbDescription 
	extends SingleRowModelAccess[Description] with NonDeprecatedView[Description] with Indexed
{
	// COMPUTED	--------------------
	
	/**
	  * Factory used for constructing database the interaction models
	  */
	protected def model = DescriptionModel
	
	
	// IMPLEMENTED	--------------------
	
	override def factory = DescriptionFactory
	
	
	// OTHER	--------------------
	
	/**
	  * @param id Database id of the targeted Description instance
	  * @return An access point to that Description
	  */
	def apply(id: Int) = DbSingleDescription(id)
}

