package utopia.logos.database.reader.url

import utopia.flow.generic.model.template.HasPropertiesLike.HasProperties
import utopia.logos.database.reader.text.TextPlacementDbReaderLike
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement

/**
  * Used for reading link placement data from the DB
  * @author Mikko Hilpinen
  * @since 11.07.2025, v0.4
  */
object LinkPlacementDbReader extends TextPlacementDbReaderLike[LinkPlacement]
{
	// ATTRIBUTES	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	override val dbProps = LinkPlacementDbModel
	
	
	// IMPLEMENTED	--------------------
	
	override def table = dbProps.table
	
	/**
	  * @param model      Model from which additional data may be read
	  * @param id         Id to assign to the read/parsed text placement
	  * @param parentId   parent id to assign to the new text placement
	  * @param placedId   placed id to assign to the new text placement
	  * @param orderIndex order index to assign to the new text placement
	  */
	override protected def apply(model: HasProperties, id: Int, parentId: Int, placedId: Int, orderIndex: Int) =
		LinkPlacement(id, LinkPlacementData(parentId, placedId, orderIndex))
}

