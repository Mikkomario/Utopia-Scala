package utopia.logos.database.access.text.placement

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.nosql.targeting.columns.AccessValues

/**
  * Used for accessing text placement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait AccessTextPlacementValues extends AccessValues
{
	// ATTRIBUTES	--------------------
	
	lazy val ids = apply(model.index).optional { _.int }
	
	/**
	  * Id of the text where the placed text appears
	  */
	lazy val parentIds = apply(model.parentId) { v => v.getInt }
	
	/**
	  * Id of the text that is placed within the parent text
	  */
	lazy val placedIds = apply(model.placedId) { v => v.getInt }
	
	/**
	  * 0-based index that indicates the specific location of the placed text
	  */
	lazy val orderIndices = apply(model.orderIndex) { v => v.getInt }
	
	
	// ABSTRACT	--------------------
	
	/**
	  * Interface for accessing text placement database properties
	  */
	def model: TextPlacementDbProps
}

