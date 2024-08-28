package utopia.logos.database.access.single.url.link.placement

import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess

/**
  * An access point to individual link placements, based on their id
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class DbSingleLinkPlacement(id: Int) 
	extends UniqueLinkPlacementAccess with SingleIntIdModelAccess[LinkPlacement]

