package utopia.logos.database.access.text.word.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.access.text.placement.AccessTextPlacementValue
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.vault.nosql.targeting.columns.AccessColumns.AccessColumn

/**
  * Used for accessing individual word placement values from the DB
  * @author Mikko Hilpinen
  * @since 10.07.2025, v0.4
  */
case class AccessWordPlacementValue(access: AccessColumn) extends AccessTextPlacementValue
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Interface for accessing word placement database properties
	  */
	override val model = WordPlacementDbModel
	
	/**
	  * Style in which this word is used in this context
	  */
	lazy val style = apply(model.style).optional { v => DisplayStyle.findForValue(v) }
}

