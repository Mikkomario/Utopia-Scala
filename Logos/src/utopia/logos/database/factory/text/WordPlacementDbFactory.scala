package utopia.logos.database.factory.text

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.storable.text.WordPlacementDbModel
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.partial.text.WordPlacementData
import utopia.logos.model.stored.text.WordPlacement
import utopia.vault.sql.OrderBy

/**
  * Used for reading word placement data from the DB
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
object WordPlacementDbFactory extends TextPlacementDbFactoryLike[WordPlacement]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	override def dbProps = WordPlacementDbModel
	
	override def defaultOrdering: Option[OrderBy] = None
	
	override def table = dbProps.table
	
	/**
	  * @param model Model from which additional data may be read
	  * @param id Id to assign to the read/parsed text placement
	  * @param parentId parent id to assign to the new text placement
	  * @param placedId placed id to assign to the new text placement
	  * @param orderIndex order index to assign to the new text placement
	  */
	override protected def apply(model: AnyModel, id: Int, parentId: Int, placedId: Int, orderIndex: Int) = 
		WordPlacement(id, WordPlacementData(parentId, placedId, orderIndex, 
			DisplayStyle.fromValue(model(dbProps.style.name))))
}

