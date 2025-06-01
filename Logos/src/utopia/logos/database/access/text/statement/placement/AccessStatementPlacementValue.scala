package utopia.logos.database.access.text.statement.placement

import utopia.logos.database.access.text.placement.AccessTextPlacementValue
import utopia.logos.database.props.text.StatementPlacementDbProps

/**
  * Used for accessing individual statement placement values from the DB
  * @author Mikko Hilpinen
  * @since 01.06.2025, v0.4
  */
trait AccessStatementPlacementValue extends AccessTextPlacementValue
{
	// ABSTRACT	--------------------
	
	/**
	  * Interface for accessing statement placement database properties
	  */
	def model: StatementPlacementDbProps
}

