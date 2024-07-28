package utopia.logos.database.access.single.url.link_placement

import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual link placements, based on their id
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class DbSingleLinkPlacement(id: Int) 
	extends UniqueLinkPlacementAccess with SingleIntIdModelAccess[LinkPlacement]

