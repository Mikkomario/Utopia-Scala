package utopia.logos.model.partial.word

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.logos.model.enumeration.DisplayStyle
import utopia.logos.model.enumeration.DisplayStyle.Default
import utopia.logos.model.factory.word.WordPlacementFactory
import utopia.logos.model.template.Placed

object WordPlacementData extends FromModelFactoryWithSchema[WordPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema =
		ModelDeclaration(Vector(
			PropertyDeclaration("statementId", IntType, Vector("statement_id")),
			PropertyDeclaration("wordId", IntType, Vector("word_id")),
			PropertyDeclaration("orderIndex", IntType, Vector("order_index")),
			PropertyDeclaration("style", IntType, Vector(), Default.id)
		))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) =
		WordPlacementData(valid("statementId").getInt, valid("wordId").getInt, valid("orderIndex").getInt,
			DisplayStyle.fromValue(valid("style")))
}

/**
  * Records when a word is used in a statement
  * @param statementId Id of the statement where the referenced word appears
  * @param wordId Id of the word that appears in the described statement
  * @param orderIndex Index at which the specified word appears within the referenced statement (0-based)
  * @param style Style in which this word is used in this context
  * @author Mikko Hilpinen
  * @since 20.03.2024, v0.2
  */
case class WordPlacementData(statementId: Int, wordId: Int, orderIndex: Int, style: DisplayStyle = Default) 
	extends WordPlacementFactory[WordPlacementData] with ModelConvertible with Placed
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("statementId" -> statementId, "wordId" -> wordId, "orderIndex" -> orderIndex, 
			"style" -> style.id))
	
	override def withOrderIndex(orderIndex: Int) = copy(orderIndex = orderIndex)
	
	override def withStatementId(statementId: Int) = copy(statementId = statementId)
	
	override def withStyle(style: DisplayStyle) = copy(style = style)
	
	override def withWordId(wordId: Int) = copy(wordId = wordId)
}

