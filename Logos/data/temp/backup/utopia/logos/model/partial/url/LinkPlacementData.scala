package utopia.logos.model.partial.url

import utopia.flow.collection.immutable.Single
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.logos.model.factory.url.LinkPlacementFactory
import utopia.logos.model.partial.text.{TextPlacementData, TextPlacementDataLike}

object LinkPlacementData extends FromModelFactoryWithSchema[LinkPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = ModelDeclaration(Vector(
		PropertyDeclaration("statementId", IntType, Vector("parentId", "parent_id", "statement_id")),
		PropertyDeclaration("linkId", IntType, Vector("link_id", "placedId", "placed_id")),
		PropertyDeclaration("orderIndex", IntType, Single("order_index"), 0)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		LinkPlacementData(valid("statementId").getInt, valid("linkId").getInt, valid("orderIndex").getInt)
}

/**
  * Places a link within a statement
  * @param statementId Id of the statement where the specified link is referenced
  * @param linkId Referenced / placed link
  * @param orderIndex 0-based index that indicates the specific location of the placed text
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class LinkPlacementData(statementId: Int, linkId: Int, orderIndex: Int = 0) 
	extends LinkPlacementFactory[LinkPlacementData] with TextPlacementData 
		with TextPlacementDataLike[LinkPlacementData]
{
	// IMPLEMENTED	--------------------
	
	override def parentId = statementId
	override def placedId = linkId
	
	override def copyTextPlacement(parentId: Int, placedId: Int, orderIndex: Int) = 
		copy(statementId = parentId, linkId = placedId, orderIndex = orderIndex)
	
	override def withLinkId(linkId: Int) = copy(linkId = linkId)
	override def withStatementId(statementId: Int) = copy(statementId = statementId)
}

