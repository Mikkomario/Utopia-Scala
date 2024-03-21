package utopia.logos.model.partial.url

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.logos.model.factory.url.LinkPlacementFactory
import utopia.logos.model.template.Placed

object LinkPlacementData extends FromModelFactoryWithSchema[LinkPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("statementId", IntType, Vector("statement_id")), 
			PropertyDeclaration("linkId", IntType, Vector("link_id")), PropertyDeclaration("orderIndex", 
			IntType, Vector("order_index"))))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		LinkPlacementData(valid("statementId").getInt, valid("linkId").getInt, valid("orderIndex").getInt)
}

/**
  * Places a link within a statement
  * @param statementId Id of the statement where the specified link is referenced
  * @param linkId Referenced link
  * @param orderIndex Index where the link appears in the statement (0-based)
  * @author Mikko Hilpinen
  * @since 20.03.2024, v1.0
  */
case class LinkPlacementData(statementId: Int, linkId: Int, orderIndex: Int) 
	extends LinkPlacementFactory[LinkPlacementData] with ModelConvertible with Placed
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("statementId" -> statementId, "linkId" -> linkId, "orderIndex" -> orderIndex))
	
	override def withLinkId(linkId: Int) = copy(linkId = linkId)
	
	override def withOrderIndex(orderIndex: Int) = copy(orderIndex = orderIndex)
	
	override def withStatementId(statementId: Int) = copy(statementId = statementId)
}

