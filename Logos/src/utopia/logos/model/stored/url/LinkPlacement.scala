package utopia.logos.model.stored.url

import utopia.logos.database.access.single.url.link_placement.DbSingleLinkPlacement
import utopia.logos.model.factory.url.LinkPlacementFactory
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.{FromIdFactory, StoredModelConvertible}

/**
  * Represents a link placement that has already been stored in the database
  * @param id id of this link placement in the database
  * @param data Wrapped link placement data
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class LinkPlacement(id: Int, data: LinkPlacementData) 
	extends StoredModelConvertible[LinkPlacementData] with LinkPlacementFactory[LinkPlacement] 
		with FromIdFactory[Int, LinkPlacement] with StoredPlaced[LinkPlacementData, Int]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this link placement in the database
	  */
	def access = DbSingleLinkPlacement(id)
	
	
	// IMPLEMENTED	--------------------
	
	override def withId(id: Int) = copy(id = id)
	
	override def withLinkId(linkId: Int) = copy(data = data.withLinkId(linkId))
	
	override def withOrderIndex(orderIndex: Int) = copy(data = data.withOrderIndex(orderIndex))
	
	override def withStatementId(statementId: Int) = copy(data = data.withStatementId(statementId))
}

