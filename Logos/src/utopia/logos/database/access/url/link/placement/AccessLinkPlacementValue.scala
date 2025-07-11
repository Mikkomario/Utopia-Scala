package utopia.logos.database.access.url.link.placement

import utopia.logos.database.access.text.placement.AccessTextPlacementValue
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn

/**
  * Used for accessing individual link placement values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessLinkPlacementValue(access: AccessColumn) extends AccessTextPlacementValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing link placement database properties
	  */
	override val model = LinkPlacementDbModel
}

