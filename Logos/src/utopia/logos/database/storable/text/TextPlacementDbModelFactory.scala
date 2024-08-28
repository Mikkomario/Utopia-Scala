package utopia.logos.database.storable.text

import utopia.flow.generic.model.immutable.Value
import utopia.logos.database.props.text.{TextPlacementDbProps, TextPlacementDbPropsWrapper}
import utopia.logos.model.partial.text.TextPlacementData
import utopia.logos.model.stored.text.TextPlacement
import utopia.vault.model.immutable.Table

object TextPlacementDbModelFactory
{
	// OTHER	--------------------
	
	/**
	  * @return A factory for constructing text placement database models
	  */
	def apply(table: Table, dbProps: TextPlacementDbProps) = TextPlacementDbModelFactoryImpl(table, dbProps)
	
	
	// NESTED	--------------------
	
	/**
	  * Used for constructing TextPlacementDbModel instances and for inserting text placements to the database
	  * @param table Table targeted by these models
	  * @param textPlacementDbProps Properties which specify how the database interactions are performed
	  * @author Mikko Hilpinen
	  * @since 27.08.2024, v0.3
	  */
	case class TextPlacementDbModelFactoryImpl(table: Table, textPlacementDbProps: TextPlacementDbProps) 
		extends TextPlacementDbModelFactory with TextPlacementDbPropsWrapper
	{
		// ATTRIBUTES	--------------------
		
		// override lazy val id = DbPropertyDeclaration("id", index)
		
		
		// IMPLEMENTED	--------------------
		
		override def apply(data: TextPlacementData) = 
			apply(None, Some(data.parentId), Some(data.placedId), Some(data.orderIndex))
		
		override def withId(id: Int) = apply(id = Some(id))
		
		/**
		  * @param orderIndex 0-based index that indicates the specific location of the placed text
		  * @return A model containing only the specified order index
		  */
		override def withOrderIndex(orderIndex: Int) = apply(orderIndex = Some(orderIndex))
		/**
		  * @param parentId Id of the text where the placed text appears
		  * @return A model containing only the specified parent id
		  */
		override def withParentId(parentId: Int) = apply(parentId = Some(parentId))
		/**
		  * @param placedId Id of the text that is placed within the parent text
		  * @return A model containing only the specified placed id
		  */
		override def withPlacedId(placedId: Int) = apply(placedId = Some(placedId))
		
		override protected def complete(id: Value, data: TextPlacementData) = TextPlacement(id.getInt, data)
		
		
		// OTHER	--------------------
		
		/**
		  * @param id text placement database id
		  * @return Constructs a new text placement database model with the specified properties
		  */
		def apply(id: Option[Int] = None, parentId: Option[Int] = None, placedId: Option[Int] = None, 
			orderIndex: Option[Int] = None): TextPlacementDbModel = 
			_TextPlacementDbModel(table, textPlacementDbProps, id, parentId, placedId, orderIndex)
	}
	
	/**
	  * Used for interacting with TextPlacements in the database
	  * @param table Table interacted with when using this model
	  * @param dbProps Configurations of the interacted database properties
	  * @param id text placement database id
	  * @author Mikko Hilpinen
	  * @since 27.08.2024, v0.3
	  */
	private case class _TextPlacementDbModel(table: Table, dbProps: TextPlacementDbProps, 
		id: Option[Int] = None, parentId: Option[Int] = None, placedId: Option[Int] = None, 
		orderIndex: Option[Int] = None) 
		extends TextPlacementDbModel
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param id Id to assign to the new model (default = currently assigned id)
		  * @param parentId parent id to assign to the new model (default = currently assigned value)
		  * @param placedId placed id to assign to the new model (default = currently assigned value)
		  * @param orderIndex order index to assign to the new model (default = currently assigned value)
		  * @return Copy of this model with the specified text placement properties
		  */
		override protected def copyTextPlacement(id: Option[Int] = id, parentId: Option[Int] = parentId, 
			placedId: Option[Int] = placedId, orderIndex: Option[Int] = orderIndex) = 
			copy(id = id, parentId = parentId, placedId = placedId, orderIndex = orderIndex)
	}
}

/**
  * Common trait for factories yielding text placement database models
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementDbModelFactory 
	extends TextPlacementDbModelFactoryLike[TextPlacementDbModel, TextPlacement, TextPlacementData]

