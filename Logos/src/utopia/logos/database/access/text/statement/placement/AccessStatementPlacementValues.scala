package utopia.logos.database.access.text.statement.placement

import utopia.logos.database.access.text.placement.AccessTextPlacementValues
import utopia.logos.database.props.text.StatementPlacementDbProps

/**
  * Used for accessing statement placement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait AccessStatementPlacementValues extends AccessTextPlacementValues
{
	// ABSTRACT	--------------------
	
	/**
	  * Interface for accessing statement placement database properties
	  */
	def model: StatementPlacementDbProps
}

