package utopia.logos.model.partial.text

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType

object TextPlacementData extends FromModelFactoryWithSchema[TextPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("parentId", IntType, Single("parent_id")), 
			PropertyDeclaration("placedId", IntType, Single("placed_id")), PropertyDeclaration("orderIndex", 
			IntType, Single("order_index"), 0)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		TextPlacementData(valid("parentId").getInt, valid("placedId").getInt, valid("orderIndex").getInt)
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new text placement
	  * @param parentId Id of the text where the placed text appears
	  * @param placedId Id of the text that is placed within the parent text
	  * @param orderIndex 0-based index that indicates the specific location of the placed text
	  * @return text placement with the specified properties
	  */
	def apply(parentId: Int, placedId: Int, orderIndex: Int = 0): TextPlacementData = 
		_TextPlacementData(parentId, placedId, orderIndex)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the text placement data trait
	  * @param parentId Id of the text where the placed text appears
	  * @param placedId Id of the text that is placed within the parent text
	  * @param orderIndex 0-based index that indicates the specific location of the placed text
	  * @author Mikko Hilpinen
	  * @since 27.08.2024
	  */
	private case class _TextPlacementData(parentId: Int, placedId: Int, orderIndex: Int = 0) 
		extends TextPlacementData
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param parentId Id of the text where the placed text appears
		  * @param placedId Id of the text that is placed within the parent text
		  * @param orderIndex 0-based index that indicates the specific location of the placed text
		  */
		override def copyTextPlacement(parentId: Int, placedId: Int, orderIndex: Int = 0) = 
			_TextPlacementData(parentId, placedId, orderIndex)
	}
}

/**
  * Places some type of text to some location within another text
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
trait TextPlacementData extends TextPlacementDataLike[TextPlacementData]

