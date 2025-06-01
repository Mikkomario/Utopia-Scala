package utopia.logos.database.access.text.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.nosql.targeting.columns.AccessValue

/**
  * Used for accessing individual text placement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait AccessTextPlacementValue extends AccessValue
{
	// ATTRIBUTES	--------------------
	
	lazy val id = apply(model.index) { _.getInt }
	
	/**
	  * Id of the text where the placed text appears
	  */
	lazy val parentId = apply(model.parentId) { v => v.getInt }
	
	/**
	  * Id of the text that is placed within the parent text
	  */
	lazy val placedId = apply(model.placedId) { v => v.getInt }
	
	/**
	  * 0-based index that indicates the specific location of the placed text
	  */
	lazy val orderIndex = apply(model.orderIndex) { v => v.getInt }
	
	
	// ABSTRACT	--------------------
	
	/**
	  * Interface for accessing text placement database properties
	  */
	def model: TextPlacementDbProps
}

