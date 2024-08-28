package utopia.logos.model.partial.text

import utopia.flow.collection.immutable.{Empty, Single}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.enumeration.DisplayStyle.Default
import utopia.logos.model.factory.text.WordPlacementFactory

object WordPlacementData extends FromModelFactoryWithSchema[WordPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("statementId", IntType, Vector("parentId", "parent_id", 
			"statement_id")), PropertyDeclaration("wordId", IntType, Vector("placedId", "placed_id", 
			"word_id")), PropertyDeclaration("orderIndex", IntType, Single("order_index"), 0), 
			PropertyDeclaration("style", IntType, Empty, Default.id)))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		WordPlacementData(valid("statementId").getInt, valid("wordId").getInt, valid("orderIndex").getInt, 
			DisplayStyle.fromValue(valid("style")))
}

/**
  * Records when a word is used in a statement
  * @param statementId Id of the statement where the referenced word appears
  * @param wordId Id of the word that appears in the described statement
  * @param orderIndex 0-based index that indicates the specific location of the placed text
  * @param style Style in which this word is used in this context
  * @author Mikko Hilpinen
  * @since 27.08.2024, v0.3
  */
case class WordPlacementData(statementId: Int, wordId: Int, orderIndex: Int = 0, 
	style: DisplayStyle = Default) 
	extends WordPlacementFactory[WordPlacementData] with TextPlacementData 
		with TextPlacementDataLike[WordPlacementData]
{
	// IMPLEMENTED	--------------------
	
	override def parentId = statementId
	override def placedId = wordId
	
	override def toModel = super[TextPlacementData].toModel ++ Model(Single("style" -> style.id))
	
	override def copyTextPlacement(parentId: Int, placedId: Int, orderIndex: Int) = 
		copy(statementId = parentId, wordId = placedId, orderIndex = orderIndex)
	
	override def withStatementId(statementId: Int) = copy(statementId = statementId)
	override def withStyle(style: DisplayStyle) = copy(style = style)
	override def withWordId(wordId: Int) = copy(wordId = wordId)
}

