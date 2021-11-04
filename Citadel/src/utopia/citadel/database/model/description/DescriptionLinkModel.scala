package utopia.citadel.database.model.description

import utopia.flow.generic.ValueConversions._
import utopia.citadel.model.cached.DescriptionLinkTable
import utopia.vault.model.immutable.Storable

/**
  * Used for interacting with description links in the database (used table may vary)
  * @author Mikko Hilpinen
  * @since 23.10.2021, v2.0
  */
case class DescriptionLinkModel(linkTable: DescriptionLinkTable, id: Option[Int] = None, targetId: Option[Int] = None,
                                descriptionId: Option[Int] = None) extends Storable
{
	// IMPLEMENTED  ----------------------------------
	
	override def table = linkTable.wrapped
	
	override def valueProperties = Vector("id" -> id, linkTable.targetLinkAttName -> targetId,
		DescriptionLinkTable.descriptionLinkAttName -> descriptionId)
	
	
	// OTHER    --------------------------------------
	
	/**
	  * @param id A description link id
	  * @return A copy of this model with that id
	  */
	def withId(id: Int) = copy(id = Some(id))
	/**
	  * @param targetId Description target id
	  * @return A copy of this model with that description target id
	  */
	def withTargetId(targetId: Int) = copy(targetId = Some(targetId))
	/**
	  * @param descriptionId A description id
	  * @return A copy of this model with that description id
	  */
	def withDescriptionId(descriptionId: Int) = copy(descriptionId = Some(descriptionId))
}