package utopia.logos.database.access.single.url.link_placement

import utopia.vault.nosql.access.single.model.distinct.SingleIntIdModelAccess
import utopia.logos.model.stored.url.LinkPlacement

/**
  * An access point to individual link placements, based on their id
  * @author Mikko Hilpinen
  * @since 16.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class DbSingleLinkPlacement(id: Int) 
	extends UniqueLinkPlacementAccess with SingleIntIdModelAccess[LinkPlacement]

