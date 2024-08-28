package utopia.logos.database.factory.url

import utopia.flow.generic.model.template.ModelLike.AnyModel
import utopia.logos.database.factory.text.TextPlacementDbFactoryLike
import utopia.logos.database.storable.url.LinkPlacementDbModel
import utopia.logos.model.partial.url.LinkPlacementData
import utopia.logos.model.stored.url.LinkPlacement
import utopia.vault.nosql.factory.row.model.FromValidatedRowModelFactory
import utopia.vault.sql.OrderBy

/**
  * Used for reading link placement data from the DB
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.3
  */
object LinkPlacementDbFactory 
	extends TextPlacementDbFactoryLike[LinkPlacement] with FromValidatedRowModelFactory[LinkPlacement]
{
	// IMPLEMENTED	--------------------
	
	/**
	  * Model that specifies how the data is read
	  */
	override def dbProps = LinkPlacementDbModel
	
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
		LinkPlacement(id, LinkPlacementData(parentId, placedId, orderIndex))
}

