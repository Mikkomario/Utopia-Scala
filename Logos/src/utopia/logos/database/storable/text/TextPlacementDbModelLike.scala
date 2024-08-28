package utopia.logos.database.storable.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.logos.database.props.text.TextPlacementDbProps
import utopia.logos.model.factory.text.TextPlacementFactory
import utopia.vault.model.immutable.Storable
import utopia.vault.model.template.{FromIdFactory, HasId}

/**
  * Common trait for database models used for interacting with text placement data in the database
  * @tparam Repr Type of this DB model
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbModelLike[+Repr] 
	extends Storable with HasId[Option[Int]] with FromIdFactory[Int, Repr] with TextPlacementFactory[Repr]
{
	// ABSTRACT	--------------------
	
	def parentId: Option[Int]
	def placedId: Option[Int]
	def orderIndex: Option[Int]
	
	/**
	  * Access to the database properties which are utilized in this model
	  */
	def dbProps: TextPlacementDbProps
	
	/**
	  * @param id Id to assign to the new model (default = currently assigned id)
	  * @param parentId parent id to assign to the new model (default = currently assigned value)
	  * @param placedId placed id to assign to the new model (default = currently assigned value)
	  * @param orderIndex order index to assign to the new model (default = currently assigned value)
	  * @return Copy of this model with the specified text placement properties
	  */
	protected def copyTextPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
		placedId: Option[Int] = placedId, orderIndex: Option[Int] = orderIndex): Repr
	
	
	// IMPLEMENTED	--------------------
	
	override def valueProperties = 
		Vector(dbProps.id.name -> id, dbProps.parentId.name -> parentId, dbProps.placedId.name -> placedId, 
			dbProps.orderIndex.name -> orderIndex)
	
	override def withId(id: Int) = copyTextPlacement(id = Some(id))
	/**
	  * @param orderIndex 0-based index that indicates the specific location of the placed text
	  * @return A new copy of this model with the specified order index
	  */
	override def withOrderIndex(orderIndex: Int) = copyTextPlacement(orderIndex = Some(orderIndex))
	/**
	  * @param parentId Id of the text where the placed text appears
	  * @return A new copy of this model with the specified parent id
	  */
	override def withParentId(parentId: Int) = copyTextPlacement(parentId = Some(parentId))
	/**
	  * @param placedId Id of the text that is placed within the parent text
	  * @return A new copy of this model with the specified placed id
	  */
	override def withPlacedId(placedId: Int) = copyTextPlacement(placedId = Some(placedId))
}

