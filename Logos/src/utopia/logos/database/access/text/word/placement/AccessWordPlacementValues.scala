package utopia.logos.database.access.text.word.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.text.placement.AccessTextPlacementValues
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.nosql.targeting.columns.AccessManyColumns

/**
  * Used for accessing word placement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
case class AccessWordPlacementValues(access: AccessManyColumns) extends AccessTextPlacementValues
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing word placement database properties
	  */
	override val model = WordPlacementDbModel
	
	/**
	  * Style in which this word is used in this context
	  */
	lazy val styles = apply(model.style) { v => DisplayStyle.fromValue(v) }
}

