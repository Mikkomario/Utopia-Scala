package utopia.logos.model.partial.text

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType

object StatementPlacementData extends FromModelFactoryWithSchema[StatementPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("parentId", IntType, Single("parent_id")), 
			PropertyDeclaration("statementId", IntType, Vector("placedId", "placed_id", "statement_id")), 
			PropertyDeclaration("orderIndex", IntType, Single("order_index"), 0)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		StatementPlacementData(valid("parentId").getInt, valid("statementId").getInt, 
			valid("orderIndex").getInt)
	
	
	// OTHER	--------------------
	
	/**
	  * Creates a new statement placement
	  * @param parentId    Id of the text where the placed text appears
	  * @param statementId Id of the statement which appears within the linked text
	  * @param orderIndex  0-based index that indicates the specific location of the placed text
	  * @return statement placement with the specified properties
	  */
	def apply(parentId: Int, statementId: Int, orderIndex: Int = 0): StatementPlacementData = 
		_StatementPlacementData(parentId, statementId, orderIndex)
	
	
	// NESTED	--------------------
	
	/**
	  * Concrete implementation of the statement placement data trait
	  * @param parentId    Id of the text where the placed text appears
	  * @param statementId Id of the statement which appears within the linked text
	  * @param orderIndex  0-based index that indicates the specific location of the placed text
	  * @author Mikko Hilpinen
	  * @since 30.12.2024
	  */
	private case class _StatementPlacementData(parentId: Int, statementId: Int, orderIndex: Int = 0) 
		extends StatementPlacementData
	{
		// IMPLEMENTED	--------------------
		
		/**
		  * @param parentId    Id of the text where the placed text appears
		  * @param statementId Id of the statement which appears within the linked text
		  * @param orderIndex  0-based index that indicates the specific location of the placed text
		  */
		override def copyStatementPlacement(parentId: Int, statementId: Int, orderIndex: Int = 0) = 
			_StatementPlacementData(parentId, statementId, orderIndex)
	}
}

/**
  * Common trait for models which are used for placing statements within various texts
  * @author Mikko Hilpinen
  * @since 30.12.2024, v0.4
  */
trait StatementPlacementData 
	extends StatementPlacementDataLike[StatementPlacementData] with TextPlacementData 
		with TextPlacementDataLike[StatementPlacementData]
{
	// IMPLEMENTED	--------------------
	
	override def placedId = statementId
}

