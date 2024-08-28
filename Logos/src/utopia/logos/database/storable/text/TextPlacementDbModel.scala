package utopia.logos.database.storable.text

import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.vault.model.immutable.Table

object TextPlacementDbModel
{
	// OTHER	--------------------
	
	/**
	  * @param table The primarily targeted table
	  * @param props Targeted database properties
	  * @return A factory used for constructing text placement models using the specified configuration
	  */
	def factory(table: Table, props: TextPlacementDbProps) = TextPlacementDbModelFactory(table, props)
}

/**
  * Common trait for database interaction models dealing with text placements
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbModel extends TextPlacementDbModelLike[TextPlacementDbModel]

