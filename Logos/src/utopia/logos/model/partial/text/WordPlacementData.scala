package utopia.logos.model.partial.text

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactoryWithSchema
import utopia.flow.generic.model.immutable.{Model, ModelDeclaration, PropertyDeclaration}
import utopia.flow.generic.model.mutable.DataType.IntType
import utopia.flow.generic.model.template.ModelConvertible
import utopia.logos.model.template.Placed

object WordPlacementData extends FromModelFactoryWithSchema[WordPlacementData]
{
	// ATTRIBUTES	--------------------
	
	override lazy val schema = 
		ModelDeclaration(Vector(PropertyDeclaration("statementId", IntType, Vector("statement_id")), 
			PropertyDeclaration("wordId", IntType, Vector("word_id")), PropertyDeclaration("orderIndex", 
			IntType, Vector("order_index"))))
	
	
	// IMPLEMENTED	--------------------
	
	override protected def fromValidatedModel(valid: Model) = 
		WordPlacementData(valid("statementId").getInt, valid("wordId").getInt, valid("orderIndex").getInt)
}

/**
  * Records when a word is used in a statement
  * @param statementId Id of the statement where the referenced word appears
  * @param wordId Id of the word that appears in the described statement
  * @param orderIndex Index at which the specified word appears within the referenced statement (0-based)
  * @author Mikko Hilpinen
  * @since 12.10.2023, Emissary Email Client v0.1, added to Logos v1.0 11.3.2024
  */
case class WordPlacementData(statementId: Int, wordId: Int, orderIndex: Int) extends ModelConvertible with Placed
{
	// IMPLEMENTED	--------------------
	
	override def toModel = 
		Model(Vector("statementId" -> statementId, "wordId" -> wordId, "orderIndex" -> orderIndex))
}

