package utopia.logos.database.reader.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.model.partial.text.TextPlacementData
import utopia.logos.model.stored.text.TextPlacement
import utopia.vault.model.immutable.Table

object TextPlacementDbReader
{
	// OTHER	--------------------
	
	/**
	  * @param table   Table from which data is read
	  * @param dbProps Database properties used when reading column data
	  * @return A factory used for parsing text placements from database model data
	  */
	def apply(table: Table, dbProps: TextPlacementDbProps): TextPlacementDbReader = 
		_TextPlacementDbReader(table, dbProps)
	
	
	// NESTED	--------------------
	
	/**
	  * @param table   Table from which data is read
	  * @param dbProps Database properties used when reading column data
	  */
	private case class _TextPlacementDbReader(table: Table, dbProps: TextPlacementDbProps) 
		extends TextPlacementDbReader
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param model      Model from which additional data may be read
		  * @param id         Id to assign to the read/parsed text placement
		  * @param parentId   parent id to assign to the new text placement
		  * @param placedId   placed id to assign to the new text placement
		  * @param orderIndex order index to assign to the new text placement
		  */
		override protected def apply(model: AnyModel, id: Int, parentId: Int, placedId: Int, orderIndex: Int) =
			TextPlacement(id, TextPlacementData(parentId, placedId, orderIndex))
	}
}

/**
  * Common trait for factories which parse text placement data from database-originated models
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
trait TextPlacementDbReader extends TextPlacementDbReaderLike[TextPlacement]

