package utopia.logos.database.access.url.link.placement

import utopia.logos.database.access.text.placement.AccessTextPlacementValues
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessManyColumns

/**
  * Used for accessing link placement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessLinkPlacementValues(access: AccessManyColumns) extends AccessTextPlacementValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing link placement database properties
	  */
	override val model = LinkPlacementDbModel
}

