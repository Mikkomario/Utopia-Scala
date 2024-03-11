package utopia.logos.model.stored.url

import utopia.logos.database.access.single.url.link_placement.DbSingleLinkPlacement
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.template.StoredPlaced
import utopia.vault.model.template.StoredModelConvertible

/**
  * Represents a link placement that has already been stored in the database
  * @param id id of this link placement in the database
  * @param data Wrapped link placement data
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class LinkPlacement(id: Int, data: LinkPlacementData)
	extends StoredModelConvertible[LinkPlacementData] with StoredPlaced[LinkPlacementData, Int]
{
	// COMPUTED	--------------------
	
	/**
	  * An access point to this link placement in the database
	  */
	def access = DbSingleLinkPlacement(id)
}

